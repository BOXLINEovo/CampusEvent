package com.campusevent.interfaces.web;

import com.campusevent.application.dto.ActivityStatistics;
import com.campusevent.application.dto.RegistrationResult;
import com.campusevent.application.security.DemoAccounts;
import com.campusevent.application.service.ActivityService;
import com.campusevent.application.service.RegistrationService;
import com.campusevent.application.service.StatisticsService;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.Registration;
import com.campusevent.interfaces.web.view.ActivityView;
import com.campusevent.interfaces.web.view.ActivityViewAssembler;
import java.time.Instant;
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
public class ActivityController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final StatisticsService statisticsService;

    public ActivityController(
            ActivityService activityService,
            RegistrationService registrationService,
            StatisticsService statisticsService) {
        this.activityService = activityService;
        this.registrationService = registrationService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/activities")
    public String list(@RequestParam(required = false) String keyword, Model model) {
        List<Activity> activities = activityService.listVisible(keyword);
        List<ActivityView> views = ActivityViewAssembler.toViews(activities, statisticsService.getAll());
        model.addAttribute("activities", views);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "activities/list";
    }

    @GetMapping("/activities/{activityId}")
    public String detail(@PathVariable UUID activityId, Model model) {
        Activity activity = activityService.getDetail(activityId);
        ActivityStatistics statistics = statisticsService.getActivityStatistics(activityId);
        Registration activeRegistration = registrationService.listByStudent(DemoAccounts.STUDENT_A).stream()
                .filter(registration -> registration.getActivityId().equals(activityId))
                .filter(Registration::isActive)
                .findFirst()
                .orElse(null);
        model.addAttribute("activity", activity);
        model.addAttribute("statistics", statistics);
        model.addAttribute("registration", activeRegistration);
        model.addAttribute("open", activity.isOpenForRegistration(Instant.now()));
        return "activities/detail";
    }

    @PostMapping("/activities/{activityId}/registrations")
    public String register(@PathVariable UUID activityId, RedirectAttributes redirectAttributes) {
        try {
            RegistrationResult result = registrationService.register(DemoAccounts.STUDENT_A, activityId);
            if (result.warning() != null && !result.warning().isBlank()) {
                redirectAttributes.addFlashAttribute("warning", result.warning());
            } else {
                redirectAttributes.addFlashAttribute("message", "报名成功");
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/activities/" + activityId;
    }
}
