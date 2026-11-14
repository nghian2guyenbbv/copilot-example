package com.example.demo.incident.checkpoint;

import com.example.demo.incident.graph.IncidentState;
import jakarta.annotation.PostConstruct;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.InterruptionMetadata;
import org.bsc.langgraph4j.checkpoint.AbstractCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * LangGraph4j checkpoint store backed by SQL Server. Every write is its own ACID transaction, so a
 * crash can never leave a half-written checkpoint, and a restarted app resumes from the last commit.
 * Checkpoints are loaded newest-first, as AbstractCheckpointSaver expects.
 */
@Component
public class SqlServerCheckpointSaver extends AbstractCheckpointSaver {

    private static final String DDL = """
            IF OBJECT_ID('dbo.lg_checkpoints', 'U') IS NULL
            CREATE TABLE dbo.lg_checkpoints (
                seq           BIGINT IDENTITY(1,1) NOT NULL,
                thread_id     NVARCHAR(100) NOT NULL,
                checkpoint_id NVARCHAR(100) NOT NULL,
                node_id       NVARCHAR(100) NULL,
                next_node_id  NVARCHAR(100) NULL,
                state         NVARCHAR(MAX) NOT NULL,
                saved_at      DATETIME2 NOT NULL,
                CONSTRAINT pk_lg_checkpoints PRIMARY KEY (thread_id, checkpoint_id)
            );
            IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_lg_checkpoints_thread_seq')
            CREATE UNIQUE INDEX ix_lg_checkpoints_thread_seq ON dbo.lg_checkpoints (thread_id, seq DESC);
            """;

    private final DataSource dataSource;
    private final ObjectStreamStateSerializer<IncidentState> serializer =
            new ObjectStreamStateSerializer<>(IncidentState::new);

    public SqlServerCheckpointSaver(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    void createTable() throws SQLException {
        try (Connection c = dataSource.getConnection(); var st = c.createStatement()) {
            st.execute(DDL);
        }
    }

    @FunctionalInterface
    private interface Tx<T> {
        T run(Connection c) throws Exception;
    }

    private <T> T inTransaction(Tx<T> work) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    protected LinkedList<Checkpoint> loadCheckpoints(RunnableConfig config) throws Exception {
        return inTransaction(c -> {
            var list = new LinkedList<Checkpoint>();
            try (var ps = c.prepareStatement("""
                    SELECT checkpoint_id, node_id, next_node_id, state, saved_at
                    FROM dbo.lg_checkpoints WHERE thread_id = ? ORDER BY seq DESC""")) {
                ps.setString(1, threadId(config));
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Checkpoint(rs.getString(1),
                                serializer.readDataFromString(rs.getString(4)).data(),
                                rs.getString(2), rs.getString(3),
                                rs.getTimestamp(5).toInstant()));
                    }
                }
            }
            return list;
        });
    }

    @Override
    protected void insertedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints,
                                      Checkpoint checkpoint) throws Exception {
        inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO dbo.lg_checkpoints (thread_id, checkpoint_id, node_id, next_node_id, state, saved_at)
                    VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME())""")) {
                ps.setString(1, threadId(config));
                ps.setString(2, checkpoint.getId());
                ps.setString(3, checkpoint.getNodeId());
                ps.setString(4, checkpoint.getNextNodeId());
                ps.setString(5, serializer.writeDataAsString(checkpoint.getState()));
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    protected void updatedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints,
                                     Checkpoint checkpoint) throws Exception {
        inTransaction(c -> {
            try (var ps = c.prepareStatement("""
                    UPDATE dbo.lg_checkpoints SET node_id = ?, next_node_id = ?, state = ?, saved_at = SYSUTCDATETIME()
                    WHERE thread_id = ? AND checkpoint_id = ?""")) {
                ps.setString(1, checkpoint.getNodeId());
                ps.setString(2, checkpoint.getNextNodeId());
                ps.setString(3, serializer.writeDataAsString(checkpoint.getState()));
                ps.setString(4, threadId(config));
                ps.setString(5, checkpoint.getId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    protected Tag releaseCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints, String message)
            throws Exception {
        inTransaction(c -> {
            try (var ps = c.prepareStatement("DELETE FROM dbo.lg_checkpoints WHERE thread_id = ?")) {
                ps.setString(1, threadId(config));
                ps.executeUpdate();
            }
            return null;
        });
        return new Tag(threadId(config), checkpoints);
    }

    @Override
    protected Tag releaseCheckpointsOnError(RunnableConfig config, LinkedList<Checkpoint> checkpoints,
                                            Throwable exception) {
        // Keep the rows on failure so the thread can be inspected or resumed.
        return new Tag(threadId(config), checkpoints);
    }

    @Override
    public <State extends AgentState> CompletableFuture<InterruptionMetadata<State>> registerInterruption(
            RunnableConfig config, InterruptionMetadata<State> metadata) {
        return CompletableFuture.completedFuture(metadata);
    }

    @Override
    public Optional<Tag> tag(RunnableConfig config, Integer version) {
        return Optional.empty(); // versioned tags are not supported
    }
}
