package com.example.learning.job.basic;

import com.example.learning.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.UUID;

@Slf4j
@Configuration
@EnableScheduling
public class UserTransformBatchJob {

    private final JobLauncher jobLauncher;
    private final Job userTransformJob;

    public UserTransformBatchJob(JobLauncher jobLauncher,
                                 @Qualifier("userTransformJob") Job userTransformJob) {
        this.jobLauncher = jobLauncher;
        this.userTransformJob = userTransformJob;
    }

    @Scheduled(fixedRate = 20000)
    public void runJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(userTransformJob, jobParameters);
            log.info("Job finished with status: {}", execution.getStatus());
        } catch (Exception e) {
            log.error("Job execution failed", e);
        }
    }
}

@Configuration
@EnableBatchProcessing
class BatchJobConfiguration {

    @Bean
    public Job userTransformJob(JobRepository jobRepository,
                                Step transformStep) {
        return new JobBuilder("userTransformJob", jobRepository)
                .start(transformStep)
                .build();
    }

    @Bean
    public Step transformStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              JdbcCursorItemReader<UserEntity> userItemReader,
                              ItemProcessor<UserEntity, UserEntity> userItemProcessor,
                              JdbcBatchItemWriter<UserEntity> userItemWriter) {
        return new StepBuilder("transformStep", jobRepository)
                .<UserEntity, UserEntity>chunk(10, transactionManager)
                .reader(userItemReader)
                .processor(userItemProcessor)
                .writer(userItemWriter)
                .build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // https://chat.deepseek.com/a/chat/s/20f89b75-bc3e-43ad-93bf-e8a06e05edb2
    // https://chatgpt.com/c/67fbd1ca-9b68-8010-966d-48849e5dc832
    @Bean
    public JdbcCursorItemReader<UserEntity> userItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<UserEntity>()
                .dataSource(dataSource)
                .name("userReader")
                .sql("SELECT id, username, password, seed FROM tbl_user WHERE processed = false")
                .rowMapper(new BeanPropertyRowMapper<>(UserEntity.class))
                .build();
    }

    @Bean
    public ItemProcessor<UserEntity, UserEntity> userItemProcessor() {
        return user -> {
            if (user.getSeed() != null) {
                user.setSeed(UUID.randomUUID().toString());
            }
            return user;
        };
    }

    @Bean
    public JdbcBatchItemWriter<UserEntity> userItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<UserEntity>()
                .dataSource(dataSource)
                .sql("UPDATE tbl_user SET seed = :seed, processed = true WHERE id = :id")
                .beanMapped()
                .build();
    }
}
