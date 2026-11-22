package com.campusevent.interfaces.web;

import com.campusevent.application.security.DemoAccounts;
import com.campusevent.application.service.ActivityService;
import com.campusevent.application.service.RegistrationService;
import com.campusevent.domain.model.Registration;
import com.campusevent.interfaces.web.view.ActivityViewAssembler;
import com.campusevent.interfaces.web.view.RegistrationView;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;
    private final ActivityService activityService;

    public RegistrationController(RegistrationService registrationService, ActivityService activityService) {
        this.registrationService = registrationService;
        this.activityService = activityService;
    }

    @GetMapping("/my/registrations")
    public String myRegistrations(Model model) {
        Map<UUID, String> titles = ActivityViewAssembler.titles(activityService.listVisible(null));
        List<RegistrationView> views = registrationService.listByStudent(DemoAccounts.STUDENT_A).stream()
                .map(registration -> new RegistrationView(
                        registration,
                        titles.getOrDefault(registration.getActivityId(), "活动")))
                .toList();
        model.addAttribute("registrations", views);
        return "registrations/my";
    }

    @PostMapping("/registrations/{registrationId}/cancel")
    public String cancel(@PathVariable UUID registrationId, RedirectAttributes redirectAttributes) {
        try {
            registrationService.cancel(DemoAccounts.STUDENT_A, registrationId);
            redirectAttributes.addFlashAttribute("message", "已取消报名");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/my/registrations";
    }
}
