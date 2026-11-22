package com.campusevent.interfaces.web;

import com.campusevent.application.service.ActivityService;
import com.campusevent.application.service.StatisticsService;
import com.campusevent.interfaces.web.view.ActivityViewAssembler;
import com.campusevent.interfaces.web.view.StatisticsView;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final ActivityService activityService;

    public StatisticsController(StatisticsService statisticsService, ActivityService activityService) {
        this.statisticsService = statisticsService;
        this.activityService = activityService;
    }

    @GetMapping("/admin/statistics")
    public String statistics(Model model) {
        Map<UUID, String> titles = ActivityViewAssembler.titles(activityService.listVisible(null));
        List<StatisticsView> views = statisticsService.getAll().stream()
                .map(statistics -> new StatisticsView(
                        statistics,
                        titles.getOrDefault(statistics.getActivityId(), "活动")))
                .toList();
        model.addAttribute("statistics", views);
        return "admin/statistics";
    }
}
