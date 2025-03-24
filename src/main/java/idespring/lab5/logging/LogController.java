package idespring.lab5.logging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@Tag(name = "Log Controller", description = "API для работы с лог-файлами")
@RestController
@RequestMapping("/logs")
public class LogController {
    private static final String LOG_FILE = "application.log";

    @Operation(
            summary = "Получить логи за указанную дату",
            description = "Фильтрует логи по заданной дате "
                    + "(формат: yyyy-MM-dd) и возвращает их в виде файла."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Логи успешно найдены и возвращены",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "204", description = "Логи за указанную дату отсутствуют"),
            @ApiResponse(responseCode = "404", description = "Файл логов не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/{date}")
    public ResponseEntity<Resource> getLogs(
            @Parameter(description = "Дата в формате yyyy-MM-dd", required = true)
            @PathVariable String date) {
        try {
            File logFile = new File(LOG_FILE);
            if (!logFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            File filteredLog = getFilteredLog(date, logFile);

            if (filteredLog.length() == 0) {
                return ResponseEntity.noContent().build();
            }

            Resource resource = new FileSystemResource(filteredLog);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filteredLog.getName())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static File getFilteredLog(String date, File logFile) throws IOException {
        File filteredLog = new File("filtered_log_" + date + ".log");
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(filteredLog))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(date)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
        return filteredLog;
    }
}