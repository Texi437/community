package cn.linli.auth;

import cn.linli.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

import static cn.linli.auth.ProfileDtos.*;
import static cn.linli.common.DatabaseValues.pageOffset;

@Service
public class ProfileService {
    private final ProfileMapper mapper;
    private final AuthService auth;
    private final Clock clock;

    public ProfileService(ProfileMapper mapper, AuthService auth, Clock clock) {
        this.mapper = mapper;
        this.auth = auth;
        this.clock = clock;
    }

    @Transactional
    public AuthDtos.MyProfile update(AuthenticatedUser user, UpdateProfile data) {
        long id = user.id();
        if (mapper.lockActiveUser(id) == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请重新登录");
        }
        mapper.updateNickname(id, data.nickname().strip());
        mapper.updateAddress(id, data.building().strip(), data.unit().strip(), data.room().strip());
        return auth.getCurrentUserProfile(user);
    }

    public Permissions permissions(AuthenticatedUser user) {
        return new Permissions(mapper.canQueryResidents(user.id(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public ResidentPage residents(AuthenticatedUser user, int page, int size) {
        requireResidentRead(user);
        long offset = pageOffset(page, size, "page须大于0，size须在1至50之间");
        return new ResidentPage(mapper.listResidents(size, offset), mapper.countResidents(), page, size);
    }

    @Transactional(readOnly = true)
    public ResidentDetail resident(AuthenticatedUser user, long id) {
        requireResidentRead(user);
        ResidentDetail resident = mapper.findResident(id);
        if (resident == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "住户不存在");
        }
        return resident;
    }

    private void requireResidentRead(AuthenticatedUser user) {
        if (!permissions(user).canQueryResidents()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权查询住户资料");
        }
    }
}
