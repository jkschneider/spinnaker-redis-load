package io.pivotal.spinnaker

import redis.clients.jedis.Jedis
import java.util.*

fun main() {
    val jedis = Jedis("localhost")

    val provider = "com.netflix.spinnaker.clouddriver.cloudfoundry.provider.CloudFoundryProvider"
    val account = "atherton"

    (0..100000).forEach { n ->
        val app = "loadtest$n"
        val cluster = "loadtest$n"

        val space = "development"
        val spaceId = UUID.randomUUID()
        val org = "development"
        val orgId = UUID.randomUUID()
        val region = "$org > $space"

        jedis.sadd("com.netflix.spinnaker.clouddriver.cloudfoundry.provider.CloudFoundryProvider:applications:members",
                "cloudfoundry:applications:$app")

        jedis.sadd("com.netflix.spinnaker.clouddriver.cloudfoundry.provider.CloudFoundryProvider:clusters:members",
                "cloudfoundry:clusters:$account:$app:$cluster")

        jedis.set("$provider:applications:attributes:cloudfoundry:applications:$app",
                """{"resource":{"name":"$app"}}""")

        jedis.set("$provider:applications:relationships:cloudfoundry:applications:$app:clusters:$account/CloudFoundryCachingAgent",
                """["cloudfoundry:clusters:$account:$app:$cluster"]""")

        jedis.set("$provider:clusters:attributes:cloudfoundry:clusters:$account:$app:$cluster",
                """{"resource":{"accountName":"$account","name":"$cluster"}}""")

        val serverGroups = (0..3).map { sequence ->
            val serverGroup = "$cluster-v00$sequence"
            val serverGroupId = UUID.randomUUID()

            jedis.sadd("com.netflix.spinnaker.clouddriver.cloudfoundry.provider.CloudFoundryProvider:serverGroups:members",
                    "cloudfoundry:serverGroups:$account:$serverGroup:$region")

            jedis.set("$provider:serverGroups:relationships:cloudfoundry:serverGroups:$account:$serverGroup:$region:loadBalancers:$account/CloudFoundryCachingAgent",
                    "[]")

            jedis.set("$provider:serverGroups:attributes:cloudfoundry:serverGroups:$account:$serverGroup:$region",
                    """
                    {
                      "resource": {
                        "account": "$account",
                        "appArtifact": {},
                        "ciBuild": {},
                        "createdTime": 1551419054000,
                        "diskQuota": 1024,
                        "droplet": {
                          "buildpacks": [
                            {
                              "buildpackName": "client-certificate-mapper=1.8.0_RELEASE container-security-provider=1.16.0_RELEASE java-buildpack=\\u001B[34mv4.16.1\\u001B[0m-offline-https://github.com/cloudfoundry/java-buildpack.git#41b8ff8 java-main java-opts java-security jvmkill-agent=1.16.0_RELEASE open-jd...",
                              "detectOutput": "client-certificate-mapper=1.8.0_RELEASE container-security-provider=1.16.0_RELEASE java-buildpack=\\u001B[34mv4.16.1\\u001B[0m-offline-https://github.com/cloudfoundry/java-buildpack.git#41b8ff8 java-main java-opts java-security jvmkill-agent=1.16.0_RELEASE open-jd...",
                              "name": "java_buildpack_offline"
                            }
                          ],
                          "id": "${UUID.randomUUID()}",
                          "name": "$serverGroup-droplet",
                          "sourcePackage": {
                            "checksum": "${UUID.randomUUID()}",
                            "checksumType": "sha256",
                            "downloadUrl": "https://api.sys.$account.cf-app.com/v3/packages/fe46dbdb-ccaa-4d6d-9baf-e76a950ec13e/download"
                          },
                          "space": {
                            "id": "$spaceId",
                            "name": "$space",
                            "organization": {
                              "id": "$orgId",
                              "name": "$org"
                            }
                          },
                          "stack": "cflinuxfs2"
                        },
                        "env": {},
                        "healthCheckHttpEndpoint": "/actuator/health",
                        "healthCheckType": "http",
                        "id": "$serverGroupId",
                        "memory": 1024,
                        "name": "$serverGroup",
                        "space": {
                          "id": "$spaceId",
                          "name": "$space",
                          "organization": {
                            "id": "$orgId",
                            "name": "$org"
                          }
                        },
                        "state": "STARTED"
                      }
                    }
                    """.trimIndent())

            jedis.set("$provider:serverGroups:relationships:cloudfoundry:serverGroups:$account:$serverGroup:$region:instances:$account/CloudFoundryCachingAgent",
                    (0..5).joinToString(",", "[", "]") { "\"cloudfoundry:instances:$account:${UUID.randomUUID()}\"" })

            serverGroup
        }

        jedis.set("$provider:clusters:relationships:cloudfoundry:clusters:$account:$app:$cluster:serverGroups:$account/CloudFoundryCachingAgent",
                serverGroups.joinToString(",", "[", "]") { serverGroup -> "\"cloudfoundry:serverGroups:$account:$serverGroup:$region\"" })
    }
}
