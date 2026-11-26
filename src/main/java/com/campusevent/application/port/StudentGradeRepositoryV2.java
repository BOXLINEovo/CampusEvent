package com.campusevent.application.port;

import com.campusevent.application.dto.StudentSummaryV2;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

public interface StudentGradeRepositoryV2 {

    OptionalInt findGrade(UUID studentId);

    List<StudentSummaryV2> findAll();
}
