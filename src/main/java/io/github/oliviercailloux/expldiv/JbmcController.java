package io.github.oliviercailloux.expldiv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class JbmcController {
	private final Path cbmcInstallPath;

	public JbmcController() {
		cbmcInstallPath = Paths.get("/home/olivier/Local/cbmc");
	}

	public void start() throws IOException, InterruptedException {
		final String cmd = cbmcInstallPath.resolve("jbmc/src/jbmc/jbmc").toString();
		final String jbmcJar = cbmcInstallPath.resolve("jbmc/lib/java-models-library/target/core-models.jar")
				.toString();
		final String ourClasses = "target/classes";

		final Builder<String> builder = ImmutableList.builder();
		builder.add(cmd);
		builder.add("target/expldiv-0.0.1-SNAPSHOT.jar");
		builder.add("--classpath");
		builder.add(jbmcJar + ":" + ourClasses);
		builder.add("--trace");
		builder.add("--xml-ui");
		builder.add("--unwinding-assertions");
		builder.add("--unwind");
		builder.add(String.valueOf(5));
		final ProcessBuilder processBuilder = new ProcessBuilder(builder.build());
		processBuilder.redirectOutput(Paths.get("out.xml").toFile());
		final Process process = processBuilder.start();
		process.waitFor();
	}

	public static void main(String[] args) throws Exception {
		new JbmcController().start();
	}
}
