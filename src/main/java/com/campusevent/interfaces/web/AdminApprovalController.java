package com.campusevent.interfaces.web;

import com.campusevent.application.service.ActivityService;
import com.campusevent.application.service.RegistrationService;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.Registration;
import com.campusevent.interfaces.web.view.RegistrationView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminApprovalController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;

    public AdminApprovalController(ActivityService activityService, RegistrationService registrationService) {
        this.activityService = activityService;
        this.registrationService = registrationService;
    }

    @GetMapping("/admin/approvals")
    public String approvals(Model model) {
        List<RegistrationView> pending = new ArrayList<>();
        for (Activity activity : activityService.listVisible(null)) {
            for (Registration registration : registrationService.listPendingByActivity(activity.getActivityId())) {
                pending.add(new RegistrationView(registration, activity.getTitle()));
            }
        }
        model.addAttribute("pending", pending);
        return "admin/approvals";
    }

    @PostMapping("/admin/registrations/{registrationId}/approve")
    public String approve(@PathVariable UUID registrationId, RedirectAttributes redirectAttributes) {
        try {
            registrationService.approve(registrationId);
            redirectAttributes.addFlashAttribute("message", "已批准");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/approvals";
    }

    @PostMapping("/admin/registrations/{registrationId}/reject")
    public String reject(
            @PathVariable UUID registrationId,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            registrationService.reject(registrationId, reason);
            redirectAttributes.addFlashAttribute("message", "已拒绝");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/approvals";
    }
}
