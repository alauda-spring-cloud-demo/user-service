CREATE TABLE user_project_ref (
  `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT,
  `project_id` INT
);

create unique index ix_user_project_ref_id on user_project_ref (`id`);