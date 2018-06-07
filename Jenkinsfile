buildMvn {
  publishModDescriptor = 'no'
  publishAPI = 'no'
  mvnDeploy = 'yes'

  doDocker = {
    buildJavaDocker {
      publishMaster = 'yes'
      healthChk = 'yes'
      healthChkCmd = 'curl -sS --fail -o /dev/null  http://localhost:8081/admin/health || exit 1'
    }
  }
}