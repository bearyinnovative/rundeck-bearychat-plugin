<#ftl attributes={"content_type":"text/html; charset=utf-8"} />

<#if executionData.job.group??>
    <#assign jobName="${executionData.job.group} / ${executionData.job.name}">
<#else>
    <#assign jobName="${executionData.job.name}">
</#if>
<#assign message="[${jobName}](${executionData.job.href}) 执行 [ #${executionData.id}](${executionData.href})">
<#if trigger == "start">
    <#assign state="任务开始">
    <#assign attachmentText="项目名字: [${executionData.project}](${executionData.context.job.serverUrl}project/${executionData.project}/jobs)">
<#elseif trigger == "failure">
    <#assign state="任务失败">
    <#assign attachmentText="项目名字: [${executionData.project}](${executionData.context.job.serverUrl}project/${executionData.project}/jobs)\r\n失败节点: ${executionData.failedNodeListString}">
<#else>
    <#assign state="任务完成">
    <#assign attachmentText="项目名字: [${executionData.project}](${executionData.context.job.serverUrl}project/${executionData.project}/jobs)">
</#if>

{
    "text": "${state}: ${message}",
    "attachments": [{
        "text": "${attachmentText}",
        "color": "${color}"}],
    "notification": "${state}: ${jobName} 执行 #${executionData.id}"
}
