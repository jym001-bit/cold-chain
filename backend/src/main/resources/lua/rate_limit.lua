--[[
令牌桶限流算法

参数说明：
KEYS[1]: 限流Key（格式：rate_limit:IP:URI）
ARGV[1]: 桶容量（capacity）
ARGV[2]: 令牌生成速率（rate，单位：个/秒）
ARGV[3]: 本次请求消耗的令牌数（requested，通常为1）
ARGV[4]: 当前时间戳（now，单位：秒）

返回值：
1 - 允许请求（令牌足够）
0 - 拒绝请求（令牌不足）

算法原理：
1. 桶中存储令牌，每次请求消耗1个令牌
2. 令牌以固定速率生成（rate = 令牌数/秒）
3. 桶有容量上限（capacity），令牌满了就不再生成
4. 可以应对突发流量：桶满时可以一次性消耗多个令牌
--]]
--使用 ip + uri 的方法放置用户频发访问攻击

local key = KEYS[1]
local capacity = tonumber(ARGV[1])      -- 桶容量
local rate = tonumber(ARGV[2])          -- 令牌生成速率（个/秒）
local requested = tonumber(ARGV[3])     -- 本次请求消耗的令牌数
local now = tonumber(ARGV[4])           -- 当前时间戳（秒）

-- 获取桶中的令牌数和上次更新时间
local bucket = redis.call('hmget', key, 'tokens', 'last_time')
local tokens = tonumber(bucket[1])
local last_time = tonumber(bucket[2])

-- 初始化：第一次请求
if tokens == nil then
    tokens = capacity
    last_time = now
end

-- 计算这段时间新增的令牌数
local delta_time = now - last_time
local new_tokens = delta_time * rate
tokens = math.min(capacity, tokens + new_tokens)

-- 判断令牌是否足够
if tokens >= requested then
    -- 令牌足够，消耗令牌
    tokens = tokens - requested
    redis.call('hmset', key, 'tokens', tokens, 'last_time', now)
    redis.call('expire', key, 3600)  -- 设置过期时间1小时
    return 1  -- 允许请求
else
    -- 令牌不足，拒绝请求
    return 0
end
