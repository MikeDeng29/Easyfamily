package com.easyfamily.auth.service;

public interface SmsSender {

    void send(String phone, String code);
}
