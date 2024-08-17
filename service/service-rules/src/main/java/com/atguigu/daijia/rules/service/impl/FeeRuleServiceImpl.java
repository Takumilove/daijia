package com.atguigu.daijia.rules.service.impl;


import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.service.FeeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class FeeRuleServiceImpl implements FeeRuleService {
    private final KieContainer kieContainer;

    // 计算订单费用
    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
        // 封装输入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDistance(calculateOrderFeeForm.getDistance());
        Date startTime = calculateOrderFeeForm.getStartTime();
        LocalTime time = LocalTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault());
        feeRuleRequest.setStartTime(time.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        feeRuleRequest.setWaitMinute(calculateOrderFeeForm.getWaitMinute());
        // Drools使用
        KieSession kieSession = kieContainer.newKieSession();

        // 封装输出对象
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        kieSession.setGlobal("feeRuleResponse", feeRuleResponse);

        kieSession.insert(feeRuleRequest);

        kieSession.fireAllRules();

        kieSession.dispose();
        // 封装数据到FeeRuleResponseVo返回
        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        BeanUtils.copyProperties(feeRuleResponse, feeRuleResponseVo);
        return feeRuleResponseVo;
    }
}
