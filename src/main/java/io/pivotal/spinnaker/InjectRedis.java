package io.pivotal.spinnaker;

import redis.clients.jedis.Jedis;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class InjectRedis {
  public static void main(String[] args) {
    Jedis jedis = new Jedis("localhost");

    String provider = "com.netflix.spinnaker.clouddriver.cloudfoundry.provider.CloudFoundryProvider";
    String account = "atherton";

    IntStream.range(1, 11).forEach(n -> {
      String app = "loadtest" + n;
      String cluster = "loadtest" + n;

      String space = "development";
      String spaceId = UUID.randomUUID().toString();
      String org = "development";
      String orgId = UUID.randomUUID().toString();
      String region = "$org > $space";

      jedis.sadd(provider + ":applications:members",
        "cloudfoundry:applications:" + app);

      jedis.sadd(provider + ":clusters:members",
        "cloudfoundry:clusters:" + account + ":" + app + ":" + cluster);

      jedis.set(provider + ":applications:attributes:cloudfoundry:applications:" + app,
        "{\"resource\":{\"name\":\"" + app + "\"}}");

      jedis.set(provider + ":applications:relationships:cloudfoundry:applications:" + app + ":clusters:" + account + "/CloudFoundryCachingAgent",
        "[\"cloudfoundry:clusters:" + account + ":" + app + ":" + cluster + "\"]");

      jedis.set(provider + ":clusters:attributes:cloudfoundry:clusters:" + account + ":" + app + ":" + cluster,
        "{\"resource\":{\"accountName\":\"" + account + "\",\"name\":\"" + cluster + "\"}}");

      Stream<String> serverGroups = IntStream.range(0, 4).mapToObj(sequence -> {
        String serverGroup = cluster + "-v00" + sequence;
        String serverGroupId = UUID.randomUUID().toString();

        jedis.sadd("com.netflix.spinnaker.clouddriver.cloudfoundry.provider.CloudFoundryProvider:serverGroups:members",
          "cloudfoundry:serverGroups:" + account + ":" + serverGroup + ":" + region);

        jedis.set(provider + ":serverGroups:relationships:cloudfoundry:serverGroups:" + account + ":" + serverGroup + ":" + region + ":loadBalancers:" + account + "/CloudFoundryCachingAgent",
          "[]");

        String body = "{\"resource\": {\"account\":\"" + account + "\",\"appArtifact\":{},\"ciBuild\":{},\"createdTime\": 1551419054000,\"diskQuota\": 1024,\"droplet\":{\"buildpacks\":[{\"buildpackName\":\"client-certificate-mapper=1.8.0_RELEASE container-security-provider=1.16.0_RELEASE java-buildpack=\\u001B[34mv4.16.1\\u001B[0m-offline-https://github.com/cloudfoundry/java-buildpack.git#41b8ff8 java-main java-opts java-security jvmkill-agent=1.16.0_RELEASE open-jd...\",\"detectOutput\": \"client-certificate-mapper=1.8.0_RELEASE container-security-provider=1.16.0_RELEASE java-buildpack=\\u001B[34mv4.16.1\\u001B[0m-offline-https://github.com/cloudfoundry/java-buildpack.git#41b8ff8 java-main java-opts java-security jvmkill-agent=1.16.0_RELEASE open-jd...\",\"name\":\"java_buildpack_offline\"}],\"id\":\"" + UUID.randomUUID().toString()  + "\",\"name\":\"" + serverGroup + "-droplet\",\"sourcePackage\":{\"checksum\":\"" + UUID.randomUUID().toString() + "\",\"checksumType\":\"sha256\",\"downloadUrl\":\"https://api.sys." + account + ".cf-app.com/v3/packages/fe46dbdb-ccaa-4d6d-9baf-e76a950ec13e/download\"},\"space\":{\"id\":\"" + spaceId + "\",\"name\":\"" + space + "\",\"organization\":{\"id\":\"" + orgId + "\",\"name\":\"" + org + "\"}},\"stack\":\"cflinuxfs2\"},\"env\":{},\"healthCheckHttpEndpoint\":\"/actuator/health\",\"healthCheckType\":\"http\",\"id\":\"" + serverGroupId + "\",\"memory\":1024,\"name\":\"" + serverGroup + "\",\"space\":{\"id\":\"" + spaceId + "\",\"name\":\"" + space + "\",\"organization\":{\"id\":\"" + orgId + "\",\"name\":\"" + org + "\"}},\"state\":\"STARTED\"}}";
        System.out.println(body);
        jedis.set(provider + ":serverGroups:attributes:cloudfoundry:serverGroups:" + account + ":" + serverGroup + ":" + region, body);

        jedis.set(provider + ":serverGroups:relationships:cloudfoundry:serverGroups:" + account + ":" + serverGroup + ":" + region + ":instances:" + account + "/CloudFoundryCachingAgent",
          IntStream.range(0, 6).mapToObj(instanceOrdinal -> "\"cloudfoundry:instances:" + account + ":" + UUID.randomUUID().toString() + "\"")
            .collect(Collectors.joining(",", "[", "]")));

        return serverGroup;
      });

      jedis.set(provider + ":clusters:relationships:cloudfoundry:clusters:" + account + ":" + app + ":" + cluster + ":serverGroups:" + account + "/CloudFoundryCachingAgent",
        serverGroups.map(sg -> "\"cloudfoundry:serverGroups:" + account + ":" + sg + ":" + region + "\"").collect(Collectors.joining(",", "[", "]")));
    });
  }
}
