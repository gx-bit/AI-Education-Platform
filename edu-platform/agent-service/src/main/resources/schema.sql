CREATE TABLE IF NOT EXISTS t_learning_plan (
  id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, title VARCHAR(120) NOT NULL,
  goal VARCHAR(500) NOT NULL, interest VARCHAR(300) NOT NULL, current_level VARCHAR(32) NOT NULL,
  weeks INT NOT NULL, hours_per_week INT NOT NULL, status VARCHAR(20) NOT NULL,
  agent_summary TEXT, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  KEY idx_plan_user_status (user_id,status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_agent_conversation (
  id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, title VARCHAR(120) NOT NULL,
  created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  KEY idx_conversation_user_time (user_id,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_agent_message (
  id VARCHAR(36) PRIMARY KEY, conversation_id VARCHAR(36) NOT NULL, user_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL, content TEXT NOT NULL, tool_name VARCHAR(60), tool_result JSON,
  model_used VARCHAR(80), latency_ms BIGINT, created_at DATETIME NOT NULL,
  KEY idx_message_conversation_time (conversation_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_learning_task (
  id VARCHAR(36) PRIMARY KEY, plan_id VARCHAR(36) NOT NULL, user_id BIGINT NOT NULL,
  week_no INT NOT NULL, course_id BIGINT, course_title VARCHAR(200), title VARCHAR(160) NOT NULL,
  description VARCHAR(800) NOT NULL, estimated_minutes INT NOT NULL, status VARCHAR(20) NOT NULL,
  completed_at DATETIME, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  KEY idx_task_plan_week (plan_id,week_no), KEY idx_task_user_status (user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_agent_action_log (
  id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, plan_id VARCHAR(36), action_type VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL, requires_confirmation TINYINT NOT NULL DEFAULT 0,
  input_json JSON, output_json JSON, created_at DATETIME NOT NULL,
  KEY idx_action_user_time (user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
