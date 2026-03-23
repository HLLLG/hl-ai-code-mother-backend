document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const message = document.getElementById('message');
    
    if (!username || !password) {
        message.textContent = '请填写用户名和密码';
        return;
    }
    
    if (password.length < 6) {
        message.textContent = '密码至少需要6个字符';
        return;
    }
    
    message.textContent = '登录中...';
    message.style.color = '#38a169';
    
    // 模拟登录请求
    setTimeout(() => {
        message.textContent = `欢迎回来，${username}！`;
        message.style.color = '#38a169';
        document.getElementById('loginForm').reset();
    }, 1000);
});