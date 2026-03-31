provider "aws" {
  region = var.aws_region
}

terraform {
  backend "s3" {
  }
  required_version = "~> 1.3"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.54.0"
    }
    vault = {
      source  = "hashicorp/vault"
      version = "~> 3.18.0"
    }
  }
}

module "secrets" {
  source = "git@github.com:companieshouse/terraform-modules//aws/ecs/secrets?ref=1.0.336"

  name_prefix = "${local.service_name}-${var.environment}"
  environment = var.environment
  kms_key_id  = data.aws_kms_key.kms_key.id
  secrets     = nonsensitive(local.service_secrets)
}

module "ecs-service" {
  source = "git@github.com:companieshouse/terraform-modules//aws/ecs/ecs-service?ref=1.0.336"

  # Environmental configuration
  environment             = var.environment
  aws_region              = var.aws_region
  aws_profile             = var.aws_profile
  vpc_id                  = data.aws_vpc.vpc.id
  ecs_cluster_id          = data.aws_ecs_cluster.ecs_cluster.id
  task_execution_role_arn = data.aws_iam_role.ecs_cluster_iam_role.arn
  batch_service           = true
  

  # ECS Task container health check
  use_task_container_healthcheck = true
  healthcheck_path               = local.healthcheck_path
  healthcheck_matcher            = local.healthcheck_matcher
  healthcheck_unhealthy_threshold   = "5"
    # Pass health check configuration, including optional startPeriod
  task_healthcheck_interval      = var.task_healthcheck_interval
  task_healthcheck_timeout       = var.task_healthcheck_timeout
  task_healthcheck_retries       = var.task_healthcheck_retries
  task_healthcheck_start_period  = var.task_healthcheck_start_period

  # Docker container details
  docker_registry   = var.docker_registry
  docker_repo       = local.docker_repo
  container_version = var.chd_order_consumer_version
  container_port    = local.container_port

  # Service configuration
  service_name                       = local.service_name
  name_prefix                        = local.name_prefix
  desired_task_count                 = var.desired_task_count
  min_task_count                     = var.min_task_count
  max_task_count                     = var.max_task_count
  required_cpus                      = var.required_cpus
  required_memory                    = var.required_memory
  service_autoscale_enabled          = var.service_autoscale_enabled
  service_autoscale_target_value_cpu = var.service_autoscale_target_value_cpu
  service_scaledown_schedule         = var.service_scaledown_schedule
  service_scaleup_schedule           = var.service_scaleup_schedule
  service_autoscale_scale_out_cooldown = var.service_autoscale_scale_out_cooldown
  service_autoscale_scale_in_cooldown  = var.service_autoscale_scale_in_cooldown
  use_capacity_provider              = var.use_capacity_provider
  use_fargate                        = var.use_fargate
  fargate_subnets                    = local.application_subnet_ids

  # Service environment variable and secret configs
  task_environment          = local.task_environment
  task_secrets              = local.task_secrets
  app_environment_filename  = local.app_environment_filename
  use_set_environment_files = local.use_set_environment_files
}

module "ecs-service-old-kafka" {
  source = "git@github.com:companieshouse/terraform-modules//aws/ecs/ecs-service?ref=1.0.336"
  count  = var.create_old_kafka_service ? 1 : 0

  # Environmental configuration
  environment             = var.environment
  aws_region              = var.aws_region
  aws_profile             = var.aws_profile
  vpc_id                  = data.aws_vpc.vpc.id
  ecs_cluster_id          = data.aws_ecs_cluster.ecs_cluster.id
  task_execution_role_arn = data.aws_iam_role.ecs_cluster_iam_role.arn
  batch_service           = true
  cloudwatch_unhealthy_host_count_enabled = false
  cloudwatch_healthy_host_count_enabled   = false
  cloudwatch_response_time_enabled        = false
  cloudwatch_http_5xx_error_count_enabled = false
  cloudwatch_alarms_enabled               = false

  # ECS Task container health check
  use_task_container_healthcheck    = true
  healthcheck_path                  = local.healthcheck_path
  healthcheck_matcher               = local.healthcheck_matcher
  health_check_grace_period_seconds = 180

  # Inherited from custom chd-order-consumer health checks
  task_healthcheck_interval      = var.task_healthcheck_interval
  task_healthcheck_timeout       = var.task_healthcheck_timeout
  task_healthcheck_retries       = var.task_healthcheck_retries
  task_healthcheck_start_period  = var.task_healthcheck_start_period

  # Docker container details
  docker_registry   = var.docker_registry
  docker_repo       = local.docker_repo
  container_version = var.chd_order_consumer_old_kafka_version
  container_port    = local.container_port

  # Service configuration
  service_name                       = local.service_name_old_kafka
  name_prefix                        = local.name_prefix
  # Service performance and scaling configs

  desired_task_count                 = var.desired_task_count
  min_task_count                     = var.min_task_count
  max_task_count                     = var.max_task_count
  required_cpus                      = var.required_cpus
  required_memory                    = var.required_memory
  service_autoscale_enabled          = var.service_autoscale_enabled
  service_autoscale_target_value_cpu = var.service_autoscale_target_value_cpu
  service_scaledown_schedule         = var.service_scaledown_schedule
  service_scaleup_schedule           = var.service_scaleup_schedule
  service_autoscale_scale_out_cooldown = var.service_autoscale_scale_out_cooldown
  service_autoscale_scale_in_cooldown  = var.service_autoscale_scale_in_cooldown
  use_capacity_provider              = var.use_capacity_provider
  use_fargate                        = var.use_fargate
  fargate_subnets                    = local.application_subnet_ids

  # Service environment variable and secret configs
  task_environment          = local.task_environment
  task_secrets              = local.task_secrets
  app_environment_filename  = local.app_environment_filename_old_kafka
  use_set_environment_files = local.use_set_environment_files
}