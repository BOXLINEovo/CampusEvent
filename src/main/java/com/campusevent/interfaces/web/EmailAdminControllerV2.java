package com.campusevent.interfaces.web;

import com.campusevent.application.port.EmailNotificationRepositoryV2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/emails-v2")
public class EmailAdminControllerV2 {

    private final EmailNotificationRepositoryV2 emailNotificationRepository;

    public EmailAdminControllerV2(EmailNotificationRepositoryV2 emailNotificationRepository) {
        this.emailNotificationRepository = emailNotificationRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("emails", emailNotificationRepository.findAll());
        return "admin/emails-v2";
    }
}
