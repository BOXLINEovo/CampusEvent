package com.campusevent.interfaces.web;

import com.campusevent.application.service.ActivityService;
import com.campusevent.application.service.StatisticsService;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.PolicyType;
import com.campusevent.interfaces.web.form.ActivityForm;
import com.campusevent.interfaces.web.view.ActivityView;
import com.campusevent.interfaces.web.view.ActivityViewAssembler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/activities")
public class AdminActivityController {

    private final ActivityService activityService;
    private final StatisticsService statisticsService;

    public AdminActivityController(ActivityService activityService, StatisticsService statisticsService) {
        this.activityService = activityService;
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public String list(Model model) {
        List<Activity> activities = activityService.listVisible(null);
        List<ActivityView> views = ActivityViewAssembler.toViews(activities, statisticsService.getAll());
        model.addAttribute("activities", views);
        return "admin/activities/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ActivityForm form = new ActivityForm();
        form.setStartAt(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0));
        model.addAttribute("activityForm", form);
        model.addAttribute("formAction", "/admin/activities");
        model.addAttribute("editing", false);
        model.addAttribute("policyTypes", PolicyType.values());
        return "admin/activities/form";
    }

    @PostMapping
    public String create(@ModelAttribute ActivityForm form, Model model, RedirectAttributes redirectAttributes) {
        try {
            activityService.create(
                    form.getTitle(),
                    form.getCategory(),
                    form.startAtInstant(),
                    form.getLocation(),
                    form.getCapacity(),
                    form.getPolicyType());
            redirectAttributes.addFlashAttribute("message", "活动已创建");
            return "redirect:/admin/activities";
        } catch (RuntimeException ex) {
            model.addAttribute("activityForm", form);
            model.addAttribute("formAction", "/admin/activities");
            model.addAttribute("editing", false);
            model.addAttribute("policyTypes", PolicyType.values());
            model.addAttribute("error", ex.getMessage());
            return "admin/activities/form";
        }
    }

    @GetMapping("/{activityId}/edit")
    public String editForm(@PathVariable UUID activityId, Model model) {
        Activity activity = activityService.getDetail(activityId);
        model.addAttribute("activityForm", ActivityForm.from(activity));
        model.addAttribute("formAction", "/admin/activities/" + activityId);
        model.addAttribute("editing", true);
        model.addAttribute("policyTypes", PolicyType.values());
        return "admin/activities/form";
    }

    @PostMapping("/{activityId}")
    public String update(
            @PathVariable UUID activityId,
            @ModelAttribute ActivityForm form,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            activityService.update(
                    activityId,
                    form.getTitle(),
                    form.getCategory(),
                    form.startAtInstant(),
                    form.getLocation(),
                    form.getCapacity(),
                    form.getPolicyType());
            redirectAttributes.addFlashAttribute("message", "活动已更新");
            return "redirect:/admin/activities";
        } catch (RuntimeException ex) {
            model.addAttribute("activityForm", form);
            model.addAttribute("formAction", "/admin/activities/" + activityId);
            model.addAttribute("editing", true);
            model.addAttribute("policyTypes", PolicyType.values());
            model.addAttribute("error", ex.getMessage());
            return "admin/activities/form";
        }
    }

    @PostMapping("/{activityId}/delete")
    public String delete(@PathVariable UUID activityId, RedirectAttributes redirectAttributes) {
        try {
            activityService.softDelete(activityId);
            redirectAttributes.addFlashAttribute("message", "活动已删除");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/activities";
    }
}
