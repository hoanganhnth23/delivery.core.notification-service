package db.migration;

import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Local shipper-profile → account mapping so offer notifications reach the shipper's own inbox. */
public class V4__shipper_identity_projection extends BaseJavaMigration {
    @Override public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS shipper_identity_projection (principal_id BIGINT PRIMARY KEY,"
                    + "legacy_user_id BIGINT NOT NULL UNIQUE,shipper_id BIGINT NOT NULL UNIQUE,mapping_version BIGINT NOT NULL,"
                    + "updated_at TIMESTAMP NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS shipper_identity_inbox_receipts (event_id UUID PRIMARY KEY,"
                    + "event_type VARCHAR(128) NOT NULL,principal_id BIGINT NOT NULL,payload_fingerprint VARCHAR(64) NOT NULL,"
                    + "processed_at TIMESTAMP NOT NULL)");
        }
    }
}
