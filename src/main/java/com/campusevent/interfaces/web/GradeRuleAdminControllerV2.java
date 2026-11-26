package com.campusevent.interfaces.web;

import com.campusevent.application.dto.GradeRuleRowV2;
import com.campusevent.application.service.ActivityService;
import com.campusevent.application.service.GradeRuleAdminServiceV2;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.ActivityGradeRuleV2;
import com.campusevent.interfaces.web.form.GradeRuleFormV2;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/grade-rules-v2")
public class GradeRuleAdminControllerV2 {

    private final ActivityService activityService;
    private final GradeRuleAdminServiceV2 gradeRuleAdminService;

    public GradeRuleAdminControllerV2(
            ActivityService activityService,
            GradeRuleAdminServiceV2 gradeRuleAdminService) {
        this.activityService = activityService;
        this.gradeRuleAdminService = gradeRuleAdminService;
    }

    @GetMapping
    public String list(Model model) {
        List<Activity> activities = activityService.listVisible(null);
        Map<UUID, ActivityGradeRuleV2> rules = gradeRuleAdminService.findAll().stream()
                .collect(Collectors.toMap(ActivityGradeRuleV2::activityId, Function.identity()));
        List<GradeRuleRowV2> rows = activities.stream()
                .map(activity -> new GradeRuleRowV2(activity, rules.get(activity.getActivityId())))
                .toList();
        model.addAttribute("rows", rows);
        model.addAttribute("gradeRuleForm", new GradeRuleFormV2());
        return "admin/grade-rules-v2";
    }

    @PostMapping
    public String save(
            @ModelAttribute GradeRuleFormV2 form,
            RedirectAttributes redirectAttributes) {
        try {
            gradeRuleAdminService.save(form.getActivityId(), form.getMinGrade(), form.getMaxGrade());
            redirectAttributes.addFlashAttribute("message", "年级规则已保存");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/grade-rules-v2";
    }
}
