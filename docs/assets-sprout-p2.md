# P2小芽静态素材说明

2026-10-30。使用内置imagegen生成，参考已讨论的B1视觉图；不是将整张效果图切成网页，也未使用外部图片地址。

工程素材：`frontend/src/assets/sprout/welcome.png`、`complete.png`；均1254×1254 RGBA，实际包含0—255透明度，单张约1MB。空状态复用welcome。生成原图仍在本机generated_images目录保留，工程使用独立副本。

原计划WebP调整为透明PNG，保留生成结果且不引入转换依赖。两图合计约2MB，大于128px实际显示所需，后续若需要公网优化可另外压缩，本轮不新增处理链。

两张图共用的最终生成提示：

> Use case: ui-mockup asset / original character illustration. The referenced image is ONLY a STYLE AND CHARACTER reference, not a target UI to preserve. Generate a brand new single isolated mascot asset, NOT a webpage or screenshot. Match the adorable two-leaf sprout character seen in the main green banner: round lime-green face, two dark oval eyes, small smile, subtle rosy cheeks, two broad asymmetric green leaves, sitting in a simple warm terracotta flowerpot with rim. No arms, no legs, no people. Soft flat storybook illustration, very subtle painterly texture, clean readable silhouette suited to displaying at 120 CSS pixels. Front facing centered full character including entire pot and leaves, about 80% of a square canvas with comfortable transparent margin. Real transparent background, no opaque square, no background landscape, no shadow rectangle, no wording, letters, numbers, icons, UI or watermark. Output only the isolated asset.

welcome附加提示：

> Welcome pose: open oval eyes and friendly small smile, both leaves perked up. Calm welcoming expression.

complete附加提示：

> Completion encouragement pose: the SAME character, green shade, two leaves and terracotta pot proportions; eyes gently closed in a happy crescent smile, cheerful but calm, with exactly two small warm yellow accent sparkles beside the leaves, all isolated on transparency. This is a still illustration, no progress or prize symbols.

输出经目视核对角色、透明背景和无文字要求；完成图包含左右黄色装饰笔触，不把具体笔触数量作为功能约束。页面文字、按钮、题目仍由Vue渲染，不烘焙进素材。

图片通过Vite本地导入打包至Web及APK，构建时核对同字节资源；这仅证明图像资源随包提供，不代表整套业务可以脱离后端离线使用。


## ui-p2-v2视觉修订素材

首页/顶部/错题/结果改为引用`frontend/src/assets/sprout/p2-scene/`中的6份本地WebP。原welcome.png/complete.png及旧包保留。新增garden、task-notebook、continue-book、avatar由imagegen参照B1生成；庭院背景无文字、角色和按钮，笔记本/书本不画题目或成绩，头像为虚构通用儿童插画。welcome/complete复用前版透明角色。

四份生成要求均为柔和绘本水彩感、米白和嫩绿、少量暖黄与陶土色，无文字、标签、水印或整页UI。garden使用横向构图、左侧留白、云与草地、右侧灌木和少量小花；notebook使用绿色线圈本配黄色铅笔与两片叶；book为绿色封皮打开的空白练习本；avatar为短发绿衣虚构儿童头像，背景由CSS提供。文字及所有操作由HTML实现。

实施方案允许尺寸压缩与格式转换，使用现有本机Pillow一次性处理，不新增项目依赖。背景1024×466，其余图最长边384；质量85。六图合计113636字节，资源明细及原图路径在本机`.local/ui-p2-alignment-assets.json`，原图副本保留于`.local/archive/ui-p2-alignment-before/generated-originals/`。以实际manifest资源大小为准。
