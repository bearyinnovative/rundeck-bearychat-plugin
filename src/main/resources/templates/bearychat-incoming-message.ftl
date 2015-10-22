<#ftl attributes={"content_type":"text/html; charset=utf-8"} />

<#if executionData.job.group??>
    <#assign jobName="${executionData.job.group} / ${executionData.job.name}">
<#else>
    <#assign jobName="${executionData.job.name}">
</#if>
<#assign message="[${jobName}](${executionData.job.href}) 第 [#${executionData.id}](${executionData.href}) 次执行">
<#if trigger == "start">
    <#assign state="任务开始">
<#elseif trigger == "failure">
    <#assign state="任务失败">
<#else>
    <#assign state="任务完成">
</#if>

{
    "text": "${state}: ${message}",
    "attachments": [{
        "text": "attachment_text",
        "color": "${color}"}],
    "notification": "${state}: ${jobName} 第 #${executionData.id} 次执行"
}
