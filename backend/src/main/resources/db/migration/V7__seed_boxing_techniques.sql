-- Boxing reference techniques for the first AI prototype: jab, cross, guard recovery.
-- Evaluation criteria must be validated by a professional instructor before use in scoring.

INSERT INTO techniques (id, discipline_id, code, name, description, difficulty, status,
                        definition_schema_version, movement_definition, required_landmarks,
                        evaluation_criteria, scoring_rules, analyzer_config)
SELECT gen_random_uuid(),
       d.id,
       'jab',
       'Jab',
       'Lead-hand straight punch thrown from the guard to disrupt distance and set up combinations.',
       'BEGINNER',
       'PUBLISHED',
       1,
       $json${
         "schemaVersion": 1,
         "primarySide": "LEFT",
         "phases": [
           {"name": "GUARD", "description": "Orthodox stance, lead hand at cheekbone height"},
           {"name": "EXTENSION", "description": "Lead arm extends straight toward the target"},
           {"name": "RETRACTION", "description": "Lead hand returns to the guard"}
         ],
         "expectedDurationMs": 400
       }$json$::jsonb,
       $json$["NOSE", "LEFT_SHOULDER", "LEFT_ELBOW", "LEFT_WRIST",
               "RIGHT_SHOULDER", "RIGHT_ELBOW", "RIGHT_WRIST",
               "LEFT_HIP", "RIGHT_HIP"]$json$::jsonb,
       $json$[
         {"code": "ELBOW_EXTENSION", "weight": 0.4, "metric": "JOINT_ANGLE",
          "landmarks": ["LEFT_SHOULDER", "LEFT_ELBOW", "LEFT_WRIST"],
          "ideal": {"value": 170, "tolerance": 10, "unit": "DEGREES"}},
         {"code": "GUARD_RETURN", "weight": 0.3, "metric": "TRAJECTORY",
          "description": "Lead hand returns to guard height within 250 ms"},
         {"code": "BALANCE", "weight": 0.3, "metric": "POSTURE",
          "description": "Head stays above the base of support through the punch"}
       ]$json$::jsonb,
       $json${"aggregation": "WEIGHTED_MEAN", "scale": {"min": 0, "max": 100},
              "passingScore": 70, "confidenceThreshold": 0.6, "minimumVisibility": 0.5}$json$::jsonb,
       $json${"analyzer": "BOXING_STRIKE_V1", "modelVersion": "mediapipe-pose-landmarker-lite-v1",
              "parameters": {"targetFrameRate": 30, "minPoseConfidence": 0.6}}$json$::jsonb
FROM disciplines d
WHERE d.code = 'boxing';

INSERT INTO techniques (id, discipline_id, code, name, description, difficulty, status,
                        definition_schema_version, movement_definition, required_landmarks,
                        evaluation_criteria, scoring_rules, analyzer_config)
SELECT gen_random_uuid(),
       d.id,
       'cross',
       'Cross',
       'Rear-hand straight punch driven by hip and shoulder rotation.',
       'BEGINNER',
       'PUBLISHED',
       1,
       $json${
         "schemaVersion": 1,
         "primarySide": "RIGHT",
         "phases": [
           {"name": "GUARD", "description": "Rear hand at cheekbone height, weight balanced"},
           {"name": "ROTATION", "description": "Hips and rear shoulder rotate toward the target"},
           {"name": "EXTENSION", "description": "Rear arm extends straight toward the target"},
           {"name": "RETRACTION", "description": "Rear hand returns to the guard"}
         ],
         "expectedDurationMs": 500
       }$json$::jsonb,
       $json$["NOSE", "LEFT_SHOULDER", "LEFT_ELBOW", "LEFT_WRIST",
               "RIGHT_SHOULDER", "RIGHT_ELBOW", "RIGHT_WRIST",
               "LEFT_HIP", "RIGHT_HIP"]$json$::jsonb,
       $json$[
         {"code": "ELBOW_EXTENSION", "weight": 0.35, "metric": "JOINT_ANGLE",
          "landmarks": ["RIGHT_SHOULDER", "RIGHT_ELBOW", "RIGHT_WRIST"],
          "ideal": {"value": 172, "tolerance": 10, "unit": "DEGREES"}},
         {"code": "HIP_ROTATION", "weight": 0.25, "metric": "JOINT_ANGLE",
          "landmarks": ["LEFT_HIP", "RIGHT_HIP", "RIGHT_SHOULDER"],
          "description": "Rear hip rotates through the punch"},
         {"code": "GUARD_RETURN", "weight": 0.2, "metric": "TRAJECTORY",
          "description": "Rear hand returns to guard height"},
         {"code": "BALANCE", "weight": 0.2, "metric": "POSTURE",
          "description": "Head stays above the base of support through the punch"}
       ]$json$::jsonb,
       $json${"aggregation": "WEIGHTED_MEAN", "scale": {"min": 0, "max": 100},
              "passingScore": 70, "confidenceThreshold": 0.6, "minimumVisibility": 0.5}$json$::jsonb,
       $json${"analyzer": "BOXING_STRIKE_V1", "modelVersion": "mediapipe-pose-landmarker-lite-v1",
              "parameters": {"targetFrameRate": 30, "minPoseConfidence": 0.6}}$json$::jsonb
FROM disciplines d
WHERE d.code = 'boxing';

INSERT INTO techniques (id, discipline_id, code, name, description, difficulty, status,
                        definition_schema_version, movement_definition, required_landmarks,
                        evaluation_criteria, scoring_rules, analyzer_config)
SELECT gen_random_uuid(),
       d.id,
       'guard-recovery',
       'Guard Recovery',
       'Returning both hands to a protected guard after a punch or defensive movement.',
       'BEGINNER',
       'PUBLISHED',
       1,
       $json${
         "schemaVersion": 1,
         "primarySide": "BOTH",
         "phases": [
           {"name": "GUARD", "description": "Both hands at cheekbone height, chin tucked"},
           {"name": "DISRUPTION", "description": "Hands displaced by a punch or defensive movement"},
           {"name": "RECOVERY", "description": "Both hands return to the guard"}
         ],
         "expectedDurationMs": 600
       }$json$::jsonb,
       $json$["NOSE", "LEFT_SHOULDER", "LEFT_ELBOW", "LEFT_WRIST",
               "RIGHT_SHOULDER", "RIGHT_ELBOW", "RIGHT_WRIST"]$json$::jsonb,
       $json$[
         {"code": "HAND_HEIGHT", "weight": 0.35, "metric": "TRAJECTORY",
          "description": "Both wrists return above shoulder height"},
         {"code": "CHIN_TUCK", "weight": 0.25, "metric": "JOINT_ANGLE",
          "landmarks": ["NOSE", "LEFT_SHOULDER", "RIGHT_SHOULDER"],
          "description": "Chin stays tucked behind the shoulders"},
         {"code": "RETURN_SPEED", "weight": 0.2, "metric": "DURATION",
          "description": "Guard is restored within 600 ms"},
         {"code": "BALANCE", "weight": 0.2, "metric": "POSTURE",
          "description": "Head stays above the base of support through the recovery"}
       ]$json$::jsonb,
       $json${"aggregation": "WEIGHTED_MEAN", "scale": {"min": 0, "max": 100},
              "passingScore": 70, "confidenceThreshold": 0.6, "minimumVisibility": 0.5}$json$::jsonb,
       $json${"analyzer": "BOXING_GUARD_V1", "modelVersion": "mediapipe-pose-landmarker-lite-v1",
              "parameters": {"targetFrameRate": 30, "minPoseConfidence": 0.6}}$json$::jsonb
FROM disciplines d
WHERE d.code = 'boxing';
