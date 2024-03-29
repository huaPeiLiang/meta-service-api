package com.meta.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.meta.mapper.AccountMapper;
import com.meta.mapper.TenantMapper;
import com.meta.model.ErrorEnum;
import com.meta.model.FastRunTimeException;
import com.meta.model.TokenResponse;
import com.meta.model.pojo.Account;
import com.meta.model.pojo.Tenant;
import com.meta.model.request.BindPhoneRequest;
import com.meta.model.request.LoginByWechatCloudDTO;
import com.meta.model.request.LoginByWechatRequest;
import com.meta.model.response.LoginByWechatResponse;
import com.meta.utils.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

@Service
@Log4j2
public class AccountServiceImpl {

    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private WechatUtil wechatUtil;
    @Autowired
    private SmsUtil smsUtil;
    @Autowired
    private RedisUtil redisUtil;

    /**
     * 微信登录
     * */
    public LoginByWechatResponse loginByWechat(LoginByWechatRequest request){
        LoginByWechatResponse response = new LoginByWechatResponse();
        // 判断是否存在账号，没有则创建账号
        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Account::getOpenid, request.getOpenid()).eq(Account::getDataIsDeleted, false);
        Account account = accountMapper.selectOne(wrapper);
        if (ObjectUtil.isEmpty(account)){
            // 创建Tenant
            Tenant tenant = Tenant.builder().build();
            tenantMapper.insert(tenant);
            // 创建Account
            account = Account.builder().openid(request.getOpenid()).unionid(request.getUnionid()).wechatAuth(true).name(request.getName()).tenantId(tenant.getId()).build();
            accountMapper.insert(account);
        }
        Long tenantId = account.getTenantId();
        TokenResponse tokenResponse = new TokenResponse();
        if (StringUtils.isEmpty(account.getPhonePrefix()) || StringUtils.isEmpty(account.getPhone())){
            // 生成未进行手机号验证的 Token
            response.setPhoneAuth(false);
            tokenResponse = JWTUtil.getAccessTokenAndRefreshToken(account.getId(), tenantId, null, null);
        }else{
            // 生成进行过手机号验证的 Token
            response.setPhoneAuth(true);
            tokenResponse = JWTUtil.getAccessTokenAndRefreshToken(account.getId(), tenantId, account.getPhonePrefix(), account.getPhone());
        }
        response.setAccessToken(tokenResponse.getAccessToken());
        response.setRefreshToken(tokenResponse.getRefreshToken());
        return response;
    }

    /**
     * 发送短信验证码
     * */
    public void sendSignUpSmsCode(String phone){
        String validateCode = CodeUtil.getNumberCode(6);
        smsUtil.singleSendMobileCode(phone, validateCode);
        String key = RedisKeys.SIGN_UP_SMS_CODE + phone + ":" + validateCode;
        redisUtil.setEx(key, validateCode, 5, TimeUnit.MINUTES);
    }

    /**
     * 验证短信验证码并绑定账号
     * */
    public TokenResponse bindPhoneCheck(BindPhoneRequest request){
        // 查询账号
        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Account::getId, request.getAccountId());
        Account account = accountMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(account)){
            throw new FastRunTimeException(ErrorEnum.帐户不存在);
        }
        // 校验手机号是否被其他账号绑定
        QueryWrapper<Account> accountWrapper = new QueryWrapper<>();
        accountWrapper.lambda().eq(Account::getPhone, request.getPhone()).ne(Account::getId, request.getAccountId()).eq(Account::getDataIsDeleted, false);
        Account phoneAccount = accountMapper.selectOne(accountWrapper);
        if (ObjectUtils.isEmpty(phoneAccount)){
            throw new FastRunTimeException(ErrorEnum.该手机号已被绑定其他账号);
        }
        // 验证短信验证码
        if (StringUtils.isEmpty(request.getPhone()) || StringUtils.isEmpty(request.getCode())){
            throw new FastRunTimeException(ErrorEnum.参数不正确);
        }
        String key = RedisKeys.SIGN_UP_SMS_CODE + request.getPhone() + ":" + request.getCode();
        if (!redisUtil.hasKey(key)){
            throw new FastRunTimeException(ErrorEnum.验证码验证失败);
        }
        // 清空历史发送的验证码
        redisUtil.deleteAll(RedisKeys.SIGN_UP_SMS_CODE + request.getPhone());
        // 绑定手机号
        account.setPhonePrefix(request.getPhonePrefix());
        account.setPhone(request.getPhone());
        accountMapper.updateById(account);
        // 生成 token
        TokenResponse tokenResponse = JWTUtil.getAccessTokenAndRefreshToken(account.getId(), request.getTenantId(), account.getPhonePrefix(), account.getPhone());
        return tokenResponse;
    }

    /**
     * 查询账户信息
     * */
    public Account getAccountById(Long accountId){
        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Account::getId, accountId).eq(Account::getDataIsDeleted, false);
        Account account = accountMapper.selectOne(wrapper);
        return account;
    }



}
