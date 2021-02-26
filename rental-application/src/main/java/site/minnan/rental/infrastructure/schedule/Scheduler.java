package site.minnan.rental.infrastructure.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.minnan.rental.application.service.BillService;
import site.minnan.rental.application.service.UtilityService;

@Component
@Slf4j
public class Scheduler {

    @Autowired
    private BillService billService;

    @Autowired
    private UtilityService utilityService;

    @Scheduled(cron = "0 0 0 * * *")
    public void billTask(){
        log.info("修改账单状态开始");
        billService.setBillUnconfirmed();
        log.info("修改账单状态结束");
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void utilityTask(){
        log.info("备份水电开始");
        utilityService.backUpUtility();
        log.info("备份水电记录结束");
    }
}
