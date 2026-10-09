package com.klaus.moply.shared;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {

	@Test
	void shouldKeepFrameworkAndInfrastructureTypesOutOfDomainAndApplication() throws Exception {
		var violations = new ArrayList<String>();
		try (var paths = Files.walk(Path.of("src/main/java/com/klaus/moply"))) {
			for (var file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
				String name = file.toString().replace('\\', '/');
				if (!name.contains("/domain/") && !name.contains("/application/"))
					continue;
				for (String line : Files.readAllLines(file)) {
					if (line.strip().startsWith("import ") && (line.contains("org.springframework.")
							|| line.contains("jakarta.") || line.matches(".*com\\.klaus\\.moply\\..*\\.infra\\..*")))
						violations.add(name + ": " + line.strip());
				}
			}
		}
		assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
	}

}
