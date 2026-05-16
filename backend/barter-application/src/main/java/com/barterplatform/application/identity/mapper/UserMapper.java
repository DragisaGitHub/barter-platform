package com.barterplatform.application.identity.mapper;

import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.api.model.CurrentUserResponse;
import com.barterplatform.api.model.MfaSettingsResponse;
import com.barterplatform.api.model.OAuthAccountResponse;
import com.barterplatform.api.model.UserResponse;
import com.barterplatform.api.model.UserSummaryResponse;
import com.barterplatform.domain.identity.entity.OAuthAccountEntity;
import com.barterplatform.domain.identity.entity.UserMfaSettingsEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {

    UserSummaryResponse toSummaryResponse(UserEntity userEntity);

    List<UserSummaryResponse> toSummaryResponseList(List<UserEntity> userEntities);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "oauthAccounts", ignore = true)
    @Mapping(target = "mfaSettings", ignore = true)
    UserResponse toResponse(UserEntity userEntity);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "oauthAccounts", ignore = true)
    @Mapping(target = "mfaSettings", ignore = true)
    CurrentUserResponse toCurrentUserResponse(UserEntity userEntity);

    List<UserResponse> toResponseList(List<UserEntity> userEntities);

    OAuthAccountResponse toOAuthAccountResponse(OAuthAccountEntity oauthAccountEntity);

    List<OAuthAccountResponse> toOAuthAccountResponseList(List<OAuthAccountEntity> oauthAccountEntities);

    MfaSettingsResponse toMfaSettingsResponse(UserMfaSettingsEntity userMfaSettingsEntity);

    default com.barterplatform.api.model.OAuthProvider map(
            com.barterplatform.domain.identity.enums.OAuthProvider oauthProvider) {
        return oauthProvider == null ? null : com.barterplatform.api.model.OAuthProvider.valueOf(oauthProvider.name());
    }

    default com.barterplatform.api.model.PreferredLanguage map(
            com.barterplatform.domain.identity.enums.PreferredLanguage preferredLanguage) {
        com.barterplatform.domain.identity.enums.PreferredLanguage resolved = preferredLanguage == null
                ? com.barterplatform.domain.identity.enums.PreferredLanguage.SR
                : preferredLanguage;
        return com.barterplatform.api.model.PreferredLanguage.valueOf(resolved.name());
    }

    default com.barterplatform.domain.identity.enums.PreferredLanguage map(
            com.barterplatform.api.model.PreferredLanguage preferredLanguage) {
        return preferredLanguage == null
                ? com.barterplatform.domain.identity.enums.PreferredLanguage.SR
                : com.barterplatform.domain.identity.enums.PreferredLanguage.valueOf(preferredLanguage.name());
    }

    default com.barterplatform.api.model.UserStatus map(com.barterplatform.domain.identity.enums.UserStatus userStatus) {
        return userStatus == null ? null : com.barterplatform.api.model.UserStatus.valueOf(userStatus.name());
    }
}

