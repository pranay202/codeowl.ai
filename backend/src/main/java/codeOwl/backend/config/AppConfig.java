package codeOwl.backend.config;

import java.util.concurrent.Executor;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAsync
@Slf4j
public class AppConfig {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean(name = "indexingExecutor")
    Executor indexingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("index-");
        executor.initialize();
        return executor;
    }

    /**
     * Spring AI 2.0 writes vector-store metadata as jsonb, while its initial table
     * schema uses json. PostgreSQL does not implicitly assign jsonb to a json
     * column, which makes every vector insert fail. Convert only the affected
     * legacy column after Spring AI has created the table; existing metadata is
     * preserved by the explicit conversion.
     */
    @Bean
    ApplicationRunner migrateVectorStoreSchema(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.ai.vectorstore.pgvector.dimensions}") int embeddingDimensions) {
        return args -> {
            Boolean tableExists = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = 'vector_store'
                    )
                    """, Boolean.class);

            if (!Boolean.TRUE.equals(tableExists)) {
                return;
            }

            String metadataType = jdbcTemplate.queryForObject("""
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'vector_store'
                      AND column_name = 'metadata'
                    """, String.class);

            if ("json".equals(metadataType)) {
                jdbcTemplate.execute("""
                        ALTER TABLE public.vector_store
                        ALTER COLUMN metadata TYPE jsonb
                        USING metadata::jsonb
                        """);
                log.info("Migrated public.vector_store.metadata from json to jsonb");
            }

            Integer storedDimensions = jdbcTemplate.queryForObject("""
                    SELECT a.atttypmod
                    FROM pg_attribute a
                    JOIN pg_class c ON c.oid = a.attrelid
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                      AND c.relname = 'vector_store'
                      AND a.attname = 'embedding'
                      AND a.attnum > 0
                      AND NOT a.attisdropped
                    """, Integer.class);

            if (storedDimensions != null && storedDimensions != embeddingDimensions) {
                Integer vectorCount = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM public.vector_store", Integer.class);
                if (vectorCount != null && vectorCount > 0) {
                    throw new IllegalStateException("The existing vector_store uses " + storedDimensions
                            + " dimensions but the configured embedding model uses " + embeddingDimensions
                            + ". Clear and re-index the repository vectors before changing embedding providers.");
                }

                jdbcTemplate.execute("ALTER TABLE public.vector_store ALTER COLUMN embedding TYPE vector("
                        + embeddingDimensions + ")");
                log.info("Updated public.vector_store embedding dimension from {} to {}", storedDimensions,
                        embeddingDimensions);
            }
        };
    }
}
