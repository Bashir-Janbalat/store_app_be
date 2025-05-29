package org.store.app.service;


public interface EmailService {

    void sendSimpleMail(String to, String subject, String text);

    void sendHtmlMail(String to, String subject, String html);

}

