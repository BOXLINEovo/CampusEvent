package com.campusevent.interfaces.web;

import com.campusevent.application.dto.GradeRuleRowV2;
import com.campusevent.application.dto.RegistrationResult;
import com.campusevent.application.dto.StudentSummaryV2;
import com.campusevent.application.port.ActivityGradeRuleRepositoryV2;
import com.campusevent.application.port.StudentGradeRepositoryV2;
import com.campusevent.application.service.ActivityService;
import com.campusevent.application.service.RegistrationService;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.ActivityGradeRuleV2;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/grade-demo-v2")
public class GradeDemoControllerV2 {

    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final StudentGradeRepositoryV2 studentGradeRepository;
    private final ActivityGradeRuleRepositoryV2 activityGradeRuleRepository;

    public GradeDemoControllerV2(
            ActivityService activityService,
            RegistrationService registrationService,
            StudentGradeRepositoryV2 studentGradeRepository,
            ActivityGradeRuleRepositoryV2 activityGradeRuleRepository) {
        this.activityService = activityService;
        this.registrationService = registrationService;
        this.studentGradeRepository = studentGradeRepository;
        this.activityGradeRuleRepository = activityGradeRuleRepository;
    }

    @GetMapping
    public String page(Model model) {
        List<StudentSummaryV2> students = studentGradeRepository.findAll();
        List<Activity> activities = activityService.listVisible(null);
        Map<UUID, ActivityGradeRuleV2> rules = activityGradeRuleRepository.findAll().stream()
                .collect(Collectors.toMap(ActivityGradeRuleV2::activityId, Function.identity()));
        List<GradeRuleRowV2> activityRows = activities.stream()
                .map(activity -> new GradeRuleRowV2(activity, rules.get(activity.getActivityId())))
                .toList();
        model.addAttribute("students", students);
        model.addAttribute("activityRows", activityRows);
        return "grade-demo-v2";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam UUID studentId,
            @RequestParam UUID activityId,
            RedirectAttributes redirectAttributes) {
        try {
            RegistrationResult result = registrationService.register(studentId, activityId);
            if (result.warning() == null || result.warning().isBlank()) {
                redirectAttributes.addFlashAttribute("message", "报名成功");
            } else {
                redirectAttributes.addFlashAttribute("warning", result.warning());
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/grade-demo-v2";
    }
}
