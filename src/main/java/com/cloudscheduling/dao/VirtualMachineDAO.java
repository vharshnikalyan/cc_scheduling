package com.cloudscheduling.dao;

import com.cloudscheduling.db.DatabaseConfig;
import com.cloudscheduling.model.VirtualMachine;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VirtualMachineDAO {
    
    public List<VirtualMachine> getAllVMs() {
        List<VirtualMachine> vms = new ArrayList<>();
        String sql = "SELECT * FROM virtual_machines ORDER BY vm_id";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                VirtualMachine vm = new VirtualMachine(
                    rs.getInt("vm_id"),
                    rs.getString("vm_name"),
                    rs.getInt("mips"),
                    rs.getInt("ram"),
                    rs.getInt("bandwidth"),
                    rs.getInt("pes_number"),
                    rs.getInt("storage")
                );
                vms.add(vm);
            }
            
            System.out.println("✅ Loaded " + vms.size() + " virtual machines from database");
            
        } catch (SQLException e) {
            System.err.println("❌ Error loading virtual machines: " + e.getMessage());
            // Fallback to in-memory generation if database fails
            vms = generateFallbackVMs();
        }
        
        return vms;
    }
    
    public void saveVMs(List<VirtualMachine> vms) {
        String sql = "INSERT INTO virtual_machines (vm_name, mips, ram, bandwidth, pes_number, storage) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (VirtualMachine vm : vms) {
                pstmt.setString(1, vm.getVmName());
                pstmt.setInt(2, vm.getMips());
                pstmt.setInt(3, vm.getRam());
                pstmt.setInt(4, vm.getBandwidth());
                pstmt.setInt(5, vm.getPesNumber());
                pstmt.setInt(6, vm.getStorage());
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            System.out.println("✅ Saved " + vms.size() + " virtual machines to database");
            
        } catch (SQLException e) {
            System.err.println("❌ Error saving virtual machines: " + e.getMessage());
        }
    }
    
    private List<VirtualMachine> generateFallbackVMs() {
        List<VirtualMachine> vms = new ArrayList<>();
        System.out.println("🔄 Using fallback VM generation...");
        
        String[] vmNames = {"Small-VM-1", "Small-VM-2", "Medium-VM-1", "Medium-VM-2", 
                           "Large-VM-1", "Large-VM-2", "HighCPU-VM-1", "HighCPU-VM-2"};
        int[] mips = {1000, 1000, 2000, 2000, 3000, 3000, 4000, 4000};
        int[] ram = {2048, 2048, 4096, 4096, 8192, 8192, 4096, 4096};
        int[] bandwidth = {1000, 1000, 2000, 2000, 4000, 4000, 2000, 2000};
        int[] pes = {2, 2, 4, 4, 8, 8, 8, 8};
        int[] storage = {10000, 10000, 20000, 20000, 40000, 40000, 20000, 20000};
        
        for (int i = 0; i < vmNames.length; i++) {
            VirtualMachine vm = new VirtualMachine(i + 1, vmNames[i], mips[i], ram[i], 
                                                 bandwidth[i], pes[i], storage[i]);
            vms.add(vm);
        }
        
        return vms;
    }
}