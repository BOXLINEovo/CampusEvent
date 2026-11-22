package com.campusevent.interfaces.web;

import com.campusevent.application.port.NotificationService;
import com.campusevent.application.security.DemoAccounts;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String list(Model model) {
        model.addAttribute("messages", notificationService.listByStudent(DemoAccounts.STUDENT_A));
        model.addAttribute("unread", notificationService.unreadCount(DemoAccounts.STUDENT_A));
        return "notifications/list";
    }

    @PostMapping("/notifications/{messageId}/read")
    public String markRead(@PathVariable UUID messageId, RedirectAttributes redirectAttributes) {
        try {
            notificationService.markRead(messageId, DemoAccounts.STUDENT_A);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/notifications";
    }
}
