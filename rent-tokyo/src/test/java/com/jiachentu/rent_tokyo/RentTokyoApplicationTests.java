package com.jiachentu.rent_tokyo;

import com.jiachentu.rent_tokyo.service.PropertyImportScheduler;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RentTokyoApplicationTests {

	@Test
	void contextLoads() {
	}

    @Test
    void scheduling_shouldBeEnabledForDailyImport() throws Exception {
        assertThat(RentTokyoApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();

        Method method = PropertyImportScheduler.class.getDeclaredMethod("runDailyImport");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${rent-tokyo.import.cron:0 0 3 * * *}");
    }

}
