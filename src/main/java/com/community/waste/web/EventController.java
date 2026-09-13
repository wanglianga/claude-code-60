package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.BucketEvent;
import com.community.waste.service.EventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final CurrentUser currentUser;

    public EventController(EventService eventService, CurrentUser currentUser) {
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','COLLECTOR','GOVERNANCE','ADMIN')")
    public List<BucketEvent> list(@RequestParam(required = false) String status) {
        return eventService.list(status);
    }

    /** 与我相关的事件（作为参与方被串联进来）。 */
    @GetMapping("/mine")
    public List<BucketEvent> mine() {
        return eventService.mine(currentUser.require());
    }

    @PostMapping("/{id}/ack")
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','COLLECTOR','GOVERNANCE','ADMIN')")
    public BucketEvent ack(@PathVariable Long id) {
        return eventService.acknowledge(id, currentUser.require());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','COLLECTOR','GOVERNANCE','ADMIN')")
    public BucketEvent resolve(@PathVariable Long id) {
        return eventService.resolve(id, currentUser.require());
    }

    /** 手动触发巡检（复核超时 / 清运车迟到 / 楼栋误投率升高）。 */
    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('PROPERTY','GOVERNANCE','ADMIN')")
    public List<BucketEvent> check() {
        return eventService.runAllChecks();
    }
}
