document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const message = document.getElementById('message');
    
    // 简单的验证逻辑
    if (username && password) {
        message.textContent = `欢迎，${username}！登录成功`;
        message.style.color = 'green';
        // 这里可以添加实际的登录逻辑
    } else {
        message.textContent = '请填写所有字段';
        message.style.color = 'red';
    }
});