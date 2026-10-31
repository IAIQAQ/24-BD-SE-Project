package cn.edu.hust.mathdrill.auth;
public record Account(long id,String loginName,String passwordHash,String role,String status,String nickname,Integer grade) {}
