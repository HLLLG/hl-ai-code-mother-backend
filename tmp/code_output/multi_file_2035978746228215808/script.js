document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const messageEl = document.getElementById('message');
    
    // 简单的验证逻辑
    if (!username || !password) {
        messageEl.textContent = '请填写用户名和密码！';
        messageEl.style.color = '#dc3545';
        return;
    }
    
    // 模拟登录成功
    messageEl.textContent = '登录成功，正在跳转...';
    messageEl.style.color = '#28a745';
    
    // 清空表单（模拟跳转前的操作）
    setTimeout(() => {
        document.getElementById('loginForm').reset();
        messageEl.textContent = '';
        alert(`欢迎回来，${username}！`);
    }, 1500);
});