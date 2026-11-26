package com.campusevent.infrastructure.persistence.jdbc;

import com.campusevent.application.dto.StudentSummaryV2;
import com.campusevent.application.port.StudentGradeRepositoryV2;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStudentGradeRepositoryV2 implements StudentGradeRepositoryV2 {

    private final JdbcTemplate jdbcTemplate;

    public JdbcStudentGradeRepositoryV2(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OptionalInt findGrade(UUID studentId) {
        List<Integer> grades = jdbcTemplate.query(
                "select grade from student_grade_v2 where student_id = ?",
                (rs, rowNum) -> rs.getInt(1),
                studentId);
        return grades.isEmpty() ? OptionalInt.empty() : OptionalInt.of(grades.get(0));
    }

    @Override
    public List<StudentSummaryV2> findAll() {
        return jdbcTemplate.query(
                """
                select s.student_id, s.name, g.grade
                from student s
                join student_grade_v2 g on g.student_id = s.student_id
                order by s.name
                """,
                (rs, rowNum) -> new StudentSummaryV2(
                        UUID.fromString(rs.getString("student_id")),
                        rs.getString("name"),
                        rs.getInt("grade")));
    }
}
