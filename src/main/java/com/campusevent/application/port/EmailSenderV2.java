package com.campusevent.application.port;

import com.campusevent.application.dto.EmailMessageV2;
import com.campusevent.application.dto.EmailSendResultV2;

public interface EmailSenderV2 {

    EmailSendResultV2 send(EmailMessageV2 message);
}
