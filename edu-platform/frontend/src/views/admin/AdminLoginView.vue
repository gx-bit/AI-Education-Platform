<template>
  <main class="admin-login">
    <section class="brand-panel">
      <div class="brand"><span class="mark"><el-icon><Grid /></el-icon></span><span>EDU OPS</span></div>
      <div class="brand-copy"><el-tag effect="dark" round>教育平台运营中枢</el-tag><h1>让每一次管理决策<br>都有数据依据</h1><p>统一管理用户、课程、交易、通知与智能推荐策略。</p></div>
      <div class="security"><el-icon><Lock /></el-icon><span>管理员专属安全入口 · 操作受权限审计保护</span></div>
    </section>
    <section class="login-panel">
      <div class="login-box"><div class="eyebrow">ADMINISTRATOR ACCESS</div><h2>管理员登录</h2><p>使用平台管理员凭据进入运营后台</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="login">
          <el-form-item label="管理员账号" prop="account"><el-input v-model="form.account" size="large" placeholder="请输入管理员账号" prefix-icon="User" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" /></el-form-item>
          <el-checkbox v-model="form.remember">在此设备保持登录</el-checkbox>
          <el-button type="primary" size="large" native-type="submit" :loading="loading">进入管理后台</el-button>
        </el-form><router-link to="/auth/login" class="user-entry">返回用户登录入口</router-link>
      </div>
    </section>
  </main>
</template>
<script setup>
import{reactive,ref}from'vue';import{useRouter}from'vue-router';import{useUserStore}from'@/stores/user';import{ElMessage}from'element-plus';
const router=useRouter(),store=useUserStore(),formRef=ref(),loading=ref(false);const form=reactive({account:'',password:'',remember:false});const rules={account:[{required:true,message:'请输入管理员账号'}],password:[{required:true,message:'请输入密码'}]};
async function login(){if(!await formRef.value.validate().catch(()=>false))return;loading.value=true;try{await store.login({account:form.account,password:form.password},form.remember,'admin');if(!store.isAdmin){await store.logout('/admin/login');ElMessage.error('该账号没有管理员权限');return}router.replace('/admin/dashboard')}finally{loading.value=false}}
</script>
<style scoped>
.admin-login{min-height:100vh;display:grid;grid-template-columns:1.08fr .92fr;background:#f6f8fc}.brand-panel{position:relative;padding:54px 64px;color:#fff;background:radial-gradient(circle at 20% 15%,#365ae8 0,transparent 30%),linear-gradient(145deg,#071226,#101f40 65%,#18366c);display:flex;flex-direction:column;justify-content:space-between;overflow:hidden}.brand-panel:after{content:'';position:absolute;width:420px;height:420px;border:1px solid #ffffff18;border-radius:50%;right:-120px;bottom:-160px;box-shadow:0 0 0 70px #ffffff08,0 0 0 140px #ffffff05}.brand{display:flex;align-items:center;gap:12px;font-weight:800;letter-spacing:2px}.mark{width:42px;height:42px;border-radius:12px;background:#4f73ff;display:grid;place-items:center}.brand-copy{position:relative;z-index:1}.brand-copy h1{font-size:46px;line-height:1.25;margin:22px 0}.brand-copy p{color:#b9c7e5;font-size:17px}.security{display:flex;align-items:center;gap:8px;color:#92a7ce;font-size:13px}.login-panel{display:grid;place-items:center;padding:50px}.login-box{width:420px}.eyebrow{color:#5271ff;font-size:12px;font-weight:800;letter-spacing:1.8px}.login-box h2{font-size:32px;margin:10px 0}.login-box>p{color:#8791a5;margin-bottom:32px}.el-button{width:100%;margin-top:24px;height:48px;background:#3157e8}.user-entry{display:block;text-align:center;margin-top:24px;color:#77839a;text-decoration:none}@media(max-width:850px){.admin-login{grid-template-columns:1fr}.brand-panel{display:none}.login-panel{padding:24px}.login-box{width:100%;max-width:420px}}
</style>
