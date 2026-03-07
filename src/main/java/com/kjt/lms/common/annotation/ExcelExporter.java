package com.kjt.lms.common.annotation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

public class ExcelExporter {
    private static final Logger logger = Logger.getLogger(ExcelExporter.class.getName());

    public static <T> ByteArrayOutputStream exportToExcel(List<T> data, InputStream templateStream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Books");

            // Handle empty data
            if (data == null || data.isEmpty()) {
                logger.warning("Data is empty, creating empty sheet");
                workbook.write(out);
                out.flush();
                return out;
            }

            try {
                // Get class and fields
                Class<?> clazz = data.getFirst().getClass();
                Field[] fields = clazz.getDeclaredFields();

                if (fields.length == 0) {
                    logger.warning("No fields found in class: " + clazz.getName());
                    workbook.write(out);
                    out.flush();
                    return out;
                }

                // Create header row
                createHeaderRow(sheet, workbook, fields);

                // Create data rows
                createDataRows(sheet, data, fields);

                // Auto-size columns
                autoSizeColumns(sheet, fields);

                workbook.write(out);
                out.flush();
                return out;

            } catch (Exception e) {
                logger.severe("Unexpected error during Excel export: " + e.getMessage());
                throw new RuntimeException("Export Excel failed: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            logger.severe("Failed to create Excel workbook: " + e.getMessage());
            throw new RuntimeException("Failed to create Excel workbook", e);
        }
    }

    private static void createHeaderRow(Sheet sheet, Workbook workbook, Field[] fields) {
        try {
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (Field field : fields) {
                field.setAccessible(true);

                if (field.isAnnotationPresent(ExcelColumn.class)) {
                    try {
                        ExcelColumn column = field.getAnnotation(ExcelColumn.class);
                        Cell cell = headerRow.createCell(column.col());
                        cell.setCellValue(column.title());
                        cell.setCellStyle(headerStyle);
                    } catch (Exception e) {
                        logger.warning("Error creating header for field: " + field.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error creating header row: " + e.getMessage());
            throw new RuntimeException("Error creating header row", e);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static <T> void createDataRows(Sheet sheet, List<T> data, Field[] fields) {
        try {
            int rowIndex = 1;
            for (T item : data) {
                try {
                    Row row = sheet.createRow(rowIndex++);
                    createRowData(row, item, fields);
                } catch (Exception e) {
                    logger.warning("Error creating row for item at index " + (rowIndex - 1) + ": " + e.getMessage());
                    // Continue processing other rows
                }
            }
        } catch (Exception e) {
            logger.severe("Error creating data rows: " + e.getMessage());
            throw new RuntimeException("Error creating data rows", e);
        }
    }

    private static <T> void createRowData(Row row, T item, Field[] fields) throws IllegalAccessException {
        for (Field field : fields) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(ExcelColumn.class)) {
                try {
                    ExcelColumn column = field.getAnnotation(ExcelColumn.class);
                    Cell cell = row.createCell(column.col());

                    Object value = field.get(item);
                    if (value != null) {
                        setCellValue(cell, value, column.type());
                    } else {
                        cell.setCellValue("");
                    }
                } catch (Exception e) {
                    logger.warning("Error setting cell value for field: " + field.getName() + ", " + e.getMessage());
                    row.getCell(field.getAnnotation(ExcelColumn.class).col()).setCellValue("");
                }
            }
        }
    }

    private static void autoSizeColumns(Sheet sheet, Field[] fields) {
        try {
            int maxCol = 0;
            for (Field field : fields) {
                if (field.isAnnotationPresent(ExcelColumn.class)) {
                    ExcelColumn column = field.getAnnotation(ExcelColumn.class);
                    maxCol = Math.max(maxCol, column.col());
                }
            }

            for (int i = 0; i <= maxCol; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception e) {
                    logger.warning("Error auto-sizing column " + i);
                }
            }
        } catch (Exception e) {
            logger.warning("Error auto-sizing columns: " + e.getMessage());
            // Non-critical error, continue
        }
    }

    private static void setCellValue(Cell cell, Object value, ColCellType type) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        try {
            switch (type) {
                case _INTEGER:
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        try {
                            cell.setCellValue(Long.parseLong(value.toString()));
                        } catch (NumberFormatException e) {
                            cell.setCellValue(value.toString());
                        }
                    }
                    break;
                case _DOLLARS:
                case _DOUBLE:
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        try {
                            cell.setCellValue(Double.parseDouble(value.toString()));
                        } catch (NumberFormatException e) {
                            cell.setCellValue(value.toString());
                        }
                    }
                    break;
                default:
                    cell.setCellValue(value.toString());
            }
        } catch (Exception e) {
            logger.warning("Error setting cell value: " + e.getMessage() + ", setting as string");
            try {
                cell.setCellValue(value.toString());
            } catch (Exception fallbackError) {
                logger.warning("Error setting cell value as string, leaving empty");
                cell.setCellValue("");
            }
        }
    }
}
