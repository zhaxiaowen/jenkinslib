// 发布后端服务 pipeline
def call(GIT_URL) {


    pipeline {
        agent {
            kubernetes {
                defaultContainer 'maven'
                yaml libraryResource('podTemplates/jenkinspython3.yml')
            }
        }


        stages {


            stage("拉取代码") {


                steps {
                        checkout([$class                           : 'GitSCM',
                                  branches                         : [[name: 'master']],
                                  doGenerateSubmoduleConfigurations: false,
                                  extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "gitbook"]],
                                  submoduleCfg                     : [],
                                  userRemoteConfigs                : [[credentialsId: "0aeecbfa-402b-4e35-bb88-8977b28e1c7c", url: "https://github.com/zhaxiaowen/study_node.git"]]
                        ])


                }
            }

            stage('生成Dockerfile') {
                steps {
                    sh '''
cat << EOF > Dockerfile
FROM fellah/gitbook
COPY gitbook /srv/gitbook
EOF'''
                }
            }

            stage("镜像构建并上传") {

                steps {
                    script {
                            container('kaniko') {
                                // 执行构建镜像及推送镜像操作
                                sh """/kaniko/executor --context  ./ --dockerfile Dockerfile --destination hub.zhaoxw.work/library/gitbook:v4 --skip-tls-verify=true"""
                            }

                    }
                }
            }



//            stage('docker镜像构建') {
//                steps {
//                    sh "cd /home/jenkins/agent/workspace"
//                    sh "docker build -f /home/jenkins/agent/workspace/Dockerfile  -t gitbook:latest /home/jenkins/agent/workspace/"
//                    echo 'docker镜像构建成功'
//                }
//            }
//            stage("镜像上传") {
//
//                steps {
//                    sh "docker login --username=admin   --password=qwe123456 hub.zhaoxw.work"
//                    sh "docker tag gitbook:latest hub.zhaoxw.work/library/gitbook:latest"
//                    sh "docker push hub.zhaoxw.work/library/gitbook:latest"
//                }
//            }



//            stage("发布") {
//
//                steps {
//                    script {
//                        if (params.isUseProLatestImage == "Y") {
//                            log.info("开始使用生产镜像发布")
//                            IMAGE_NAME = opsapi.getLatestDeployVersion(BUSINESSLINE, MODEL)
//                            log.info("获取的生产镜像地址：${IMAGE_NAME}")
//                        }
//
//                        log.info("开始在${PROVIDER_CLOUD_LIST}云上部署${MODEL}服务的：${DEPLOY_CONTROLLER_LIST}控制器！")
//                        def CLOUD_DIR = ""
//                        for (PROVIDER_CLOUD in PROVIDER_CLOUD_LIST) {
//                            if (PROVIDER_CLOUD == "TencentCloud") {
//                                CLOUD_DIR = "tencent"
//                            } else if (PROVIDER_CLOUD == "HuaWeiCloud") {
//                                CLOUD_DIR = "huawei"
//                            }
//
//                            log.info("开始执行 ${PROVIDER_CLOUD} 环境的发布")
//                            def cloud_source_dir = "${c.K8S_CONTROLLER_RESOURCES_DIR}/business/${BUSINESSLINE.toLowerCase()}/${ENV_NAME}/${PROJECT}/${TYPE}/${MODEL}/${CLOUD_DIR}"
//
//                            for (DEPLOY_CONTROLLER in DEPLOY_CONTROLLER_LIST) {
//                                log.info("准备开始发布${DEPLOY_CONTROLLER}控制器")
//
//                                def controller_file_list = sh(returnStdout: true, script: "find ${cloud_source_dir}/ -name \"${DEPLOY_CONTROLLER}*\" ").split()
//
//                                if (controller_file_list.size() < 1) {
//                                    ws_log.error_out("请联系运维同事检查yaml文件是否在git存在，如果不存在，则运维同事使用 init_controller_yml 这个jenkins job来创建相关资源。")
//                                }
//
//                                if (DEPLOY_CONTROLLER == "configmap") {
//                                    for (controller_file in controller_file_list) {
//                                        log.info("准备发布${DEPLOY_CONTROLLER}控制器文件：${controller_file}")
//                                        sh "cat ${controller_file}"
//
//                                        def body_yaml = readYaml file: controller_file
//                                        def body = readFile file: controller_file
//                                        def NAMESPACE = body_yaml.metadata.namespace
//                                        def NAME = body_yaml.metadata.name
//                                        k8s.deployConfigmap(ENV_NAME, NAMESPACE, NAME, body, PROVIDER_CLOUD)
//                                    }
//                                }
//
//                                if (DEPLOY_CONTROLLER == "deployment") {
//                                    for (controller_file in controller_file_list) {
//                                        log.info("准备发布${DEPLOY_CONTROLLER}控制器文件：${controller_file}")
//
//                                        // deployment 需要替换下镜像名称
//                                        k8s.BackendDeploymentModify(MODEL, IMAGE_NAME, controller_file)
//                                        log.info("替换关键字后的${DEPLOY_CONTROLLER} yaml文件信息如下：")
//                                        sh "cat ${controller_file}"
//
//                                        def body = readFile file: controller_file
//                                        def body_yaml = readYaml file: controller_file
//                                        def NAME = body_yaml.metadata.name
//                                        def NAMESPACE = body_yaml.metadata.namespace
//                                        k8s.deployDeployment(ENV_NAME, NAMESPACE, NAME, body, PROVIDER_CLOUD)
//
//                                        // 把yaml文件回写到gitlab
//                                        def git_file_path = controller_file - "${c.K8S_CONTROLLER_RESOURCES_DIR}/"
//                                        container('jenkinspython3') {
//                                            pushFileChangeToGitlab(GIT_ID, git_file_path, body)
//                                        }
//                                    }
//                                }
//
//                                if (DEPLOY_CONTROLLER == "statefulset") {
//                                    for (controller_file in controller_file_list) {
//                                        log.info("准备发布${DEPLOY_CONTROLLER}控制器文件：${controller_file}")
//                                        sh "cat ${controller_file}"
//
//                                        // statefulset 需要替换下镜像名称
//                                        k8s.BackendDeploymentModify(MODEL, IMAGE_NAME, controller_file)
//                                        log.info("替换关键字后的${DEPLOY_CONTROLLER} yaml文件信息如下：")
//                                        sh "cat ${controller_file}"
//
//                                        def body = readFile file: controller_file
//                                        def body_yaml = readYaml file: controller_file
//                                        def NAME = body_yaml.metadata.name
//                                        def NAMESPACE = body_yaml.metadata.namespace
//                                        k8s.deployStatefulSet(ENV_NAME, NAMESPACE, NAME, body, PROVIDER_CLOUD)
//
//                                        // 把yaml文件回写到gitlab
//                                        def git_file_path = controller_file - "${c.K8S_CONTROLLER_RESOURCES_DIR}/"
//                                        container('jenkinspython3') {
//                                            pushFileChangeToGitlab(GIT_ID, git_file_path, body)
//                                        }
//                                    }
//                                }
//
//                                if (DEPLOY_CONTROLLER == "service") {
//                                    for (controller_file in controller_file_list) {
//                                        log.info("准备发布${DEPLOY_CONTROLLER}控制器文件：${controller_file}")
//                                        sh "cat ${controller_file}"
//
//                                        def BODY_YAML = readYaml file: controller_file
//                                        def BODY = readFile file: controller_file
//                                        def NAMESPACE = BODY_YAML.metadata.namespace
//                                        def NAME = BODY_YAML.metadata.name
//                                        k8s.deployService(ENV_NAME, NAMESPACE, NAME, BODY, BODY_YAML, PROVIDER_CLOUD)
//                                    }
//
//                                    if (CLOUD_DIR == "tencent") {
//                                        TOHER_CLOUD_DIR = "huawei"
//                                        TOHER_PROVIDER_CLOUD = "HuaWeiCloud"
//                                    } else if (CLOUD_DIR == "huawei") {
//                                        TOHER_CLOUD_DIR = "tencent"
//                                        TOHER_PROVIDER_CLOUD = "TencentCloud"
//                                    }
//
//                                    def other_cloud_source_dir = "${c.K8S_CONTROLLER_RESOURCES_DIR}/business/${BUSINESSLINE.toLowerCase()}/${ENV_NAME}/${PROJECT}/${TYPE}/${MODEL}/${TOHER_CLOUD_DIR}"
//
//                                    def other_cloud_controller_file_list = []
//                                    try {
//                                        other_cloud_controller_file_list = sh(returnStdout: true, script: "find ${other_cloud_source_dir}/ -name \"${DEPLOY_CONTROLLER}*\" ").split()
//                                    } catch (Exception e) {
//                                        log.info("不存在跨云的servcie，故不执行生成跨云或跨namespace的service操作。")
//                                        continue
//                                    }
//
//                                    for (controller_file in other_cloud_controller_file_list) {
//                                        log.info("准备发布${DEPLOY_CONTROLLER}控制器文件：${controller_file}")
//                                        sh "cat ${controller_file}"
//
//                                        def BODY_YAML = readYaml file: controller_file
//                                        def BODY = readFile file: controller_file
//                                        def NAMESPACE = BODY_YAML.metadata.namespace
//                                        def NAME = BODY_YAML.metadata.name
//                                        k8s.deployService(ENV_NAME, NAMESPACE, NAME, BODY, BODY_YAML, TOHER_PROVIDER_CLOUD)
//                                    }
//
//                                }
//                            }
//                        }
//                    }
//                }
//            }

        }
    }
}