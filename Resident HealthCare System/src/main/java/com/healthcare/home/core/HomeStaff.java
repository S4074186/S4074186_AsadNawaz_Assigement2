//package com.healthcare.home.core;
//
//import java.io.Serializable;
//import java.util.*;
//
//import com.healthcare.home.staff.Doctor;
//import com.healthcare.home.staff.Manager;
//import com.healthcare.home.staff.Nurse;
//import com.healthcare.home.staff.Staff;
//
//public class HomeStaff implements Serializable {
//
//    private final Map<String, Staff> map = new HashMap<>();
//
//    public void addMember(Staff staff) {
//        map.put(staff.getId(), staff);
//    }
//    public Optional<Staff> getById(String id) {
//        return Optional.ofNullable(map.get(id));
//    }
//
//    public static HomeStaff healthCareStaff() {
//        HomeStaff homeStaff = new HomeStaff();
//        homeStaff.addMember(new Manager("MNG-1", "Manager-1", "mng1", "mng1pwd" ));
//        homeStaff.addMember(new Nurse("NRS-1", "Nurse-1", "nrs1", "nrs1pwd" ));
//        homeStaff.addMember(new Doctor("DTR-1", "Doctor-1", "dtr1", "dtr1pwd" ));
//        return homeStaff;
//    }
//}
