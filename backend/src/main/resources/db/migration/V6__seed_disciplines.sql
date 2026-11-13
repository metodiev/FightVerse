-- Reference disciplines. Adding a new martial art is data, not code.

INSERT INTO disciplines (id, code, name, description, sort_order)
VALUES (gen_random_uuid(), 'boxing', 'Boxing',
        'Striking discipline built on punches, footwork, head movement, and ring generalship.', 10),
       (gen_random_uuid(), 'karate', 'Karate',
        'Japanese striking discipline emphasising linear techniques, kata, and kumite.', 20),
       (gen_random_uuid(), 'kickboxing', 'Kickboxing',
        'Stand-up striking discipline combining punches and kicks.', 30),
       (gen_random_uuid(), 'taekwondo', 'Taekwondo',
        'Korean striking discipline known for fast, high kicking techniques.', 40),
       (gen_random_uuid(), 'muay-thai', 'Muay Thai',
        'Thai striking discipline using punches, kicks, knees, and elbows in the clinch.', 50),
       (gen_random_uuid(), 'judo', 'Judo',
        'Japanese grappling discipline centred on throws, trips, and ground control.', 60),
       (gen_random_uuid(), 'brazilian-jiu-jitsu', 'Brazilian Jiu-Jitsu',
        'Ground-focused grappling discipline using leverage, positional control, and submissions.', 70),
       (gen_random_uuid(), 'wrestling', 'Wrestling',
        'Grappling discipline built on takedowns, control, and escapes.', 80);
