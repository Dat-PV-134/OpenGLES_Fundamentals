package com.rekoj.opengles_fundamentals

import android.content.Context
import android.opengl.GLES32
import android.opengl.GLES32.GL_COLOR_BUFFER_BIT
import android.opengl.GLES32.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES32.GL_DEPTH_TEST
import android.opengl.GLES32.glClear
import android.opengl.GLES32.glEnable
import android.opengl.GLES32.glViewport
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import com.rekoj.opengles_fundamentals.util.LoggerConfig
import com.rekoj.opengles_fundamentals.util.ShaderHelper
import com.rekoj.opengles_fundamentals.util.ShaderReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.Calendar
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer(private val context: Context) : Renderer {
    // Dữ liệu đỉnh
    private val vertices: FloatArray = floatArrayOf(
        0.5f, 0.0f, 0.0f, 0.5f, 1.0f, 0.5f, 1.0f,    // x, y, z, r, g, b, a
        0.0f, 0.75f, 0.0f, 0.5f, 0.8f, 1.0f, 1.0f,
        -0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 1.0f, 1.0f,

        0.0f, -0.75f, 0.0f, 1.0f, 0.8f, 0.5f, 1.0f,
        0.0f, 0.0f, -0.5f, 1.0f, 0.5f, 0.5f, 1.0f,
        0.0f, 0.0f, 0.5f, 0.5f, 1.0f, 1.0f, 1.0f,
    )

    // Index của các đỉnh cần vẽ
    private val indices: IntArray = intArrayOf(
        0, 1, 5,
        5, 3, 0,

        5, 1, 2,
        2, 3, 5,

        4, 1, 0,
        0, 3, 4,

        4, 1, 2,
        2, 3, 4
    )

    // Mảng để lưu trữ ma trận chiếu
    private val projectionMatrix = FloatArray(16)
    // Tương tự với ma trận setup camera
    private val viewMatrix = FloatArray(16)
    // Tương tự với ma trận biến đổi đối tượng
    private val modelMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val translateMatrix = FloatArray(16)

    private var timeElapsed: Float = 0.0f
    private var animationSpeed: Float = 0.5f

    // Các biến để lưu trữ id của program, vbo và vao
    private var program = 0
    private var VBO = 0
    private var VAO = 0
    private var EBO = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // glClearColor chỉ định giá trị màu rgba cho bộ đệm màu sắc của OpenGLES
//        glClearColor(1.0f, 0.5f, 0.5f, 0f)
        glEnable(GL_DEPTH_TEST)

        // Biên dịch và liên kết Vertex Shader và Fragment Shader vào Open GL program
        val vertexShaderSource = ShaderReader.readTextFileFromResource(context, R.raw.vertex_shader)
        val fragmentShaderSource = ShaderReader.readTextFileFromResource(context, R.raw.fragment_shader)
        program = ShaderHelper.buildProgram(vertexShaderSource, fragmentShaderSource)
        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program)
        }
        // Sử dụng program
        GLES32.glUseProgram(program)

        // Tạo 3 biến IntBuffer để lưu trữ id của VAO, VBO, EBO
        val vaoBuffer = IntBuffer.allocate(1)
        val vboBuffer = IntBuffer.allocate(1)
        val eboBuffer = IntBuffer.allocate(1)
        // Tạo VAO và lưu id lưu id vào vaoBuffer
        GLES32.glGenVertexArrays(1, vaoBuffer)
        // Tạo VBO và lưu id lưu id vào vboBuffer
        GLES32.glGenBuffers(1, vboBuffer)
        // Tạo EBO và lưu id lưu id vào vboBuffer
        GLES32.glGenBuffers(1, eboBuffer)
        // Gán id của VAO, VBO và EBO vào 3 biến toàn cục đã tạo nhằm sử dụng sau
        VAO = vaoBuffer.get(0)
        VBO = vboBuffer.get(0)
        EBO = eboBuffer.get(0)

        // Liên kết VAO để bắt đầu lưu trữ cách thức truy cập cũng như dữ liệu của VBO
        GLES32.glBindVertexArray(VAO)
        // Liên kết VBO và truyền dữ liệu đỉnh vào
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, VBO)
        // Tạo 1 vùng nhớ native để lưu trữ dữ liệu đỉnh
        val vertexBuffer: FloatBuffer = ByteBuffer
            .allocateDirect(vertices.size * Float.SIZE_BYTES) // Phân bổ vùng nhớ có kích thước = số lượng đỉnh * kích thước của 1 biến Float
            .order(ByteOrder.nativeOrder()) // Tùy vào kiến trúc của phần cứng mà vùng nhớ đc cung cấp sẽ được sắp xếp theo thứ tự khác nhau. Do đó cần sử dụng chung 1 cách sắp xếp
            .asFloatBuffer() // Chuyển ByteBuffer sang FloatBuffer để sử dụng
        // Truyền dữ liệu đỉnh vào FloatBuffer vừa tạo
        vertexBuffer.put(vertices)
        // Đặt position bằng 0 để bắt đầu đọc dữ liệu từ giá trị đầu tiên
        vertexBuffer.position(0)
        // Gửi dữ liệu đỉnh đến VBO
        // glBufferData(GLenum target, GLsizeiptr size, java.nio.Buffer data, GLenum usage);
        // target: Buffer object được chỉ định
        // size: Số byte cần thiết để lưu trữ dữ liệu đỉnh
        // data: Bộ đệm chứa dữ liệu đỉnh
        // usage: Cách sử dụng dữ liệu
        // Đối với GL_STATIC_DRAW: sửa đổi dữ liệu 1 lần và sử dụng nhiều lần
        // https://registry.khronos.org/OpenGL-Refpages/es3/html/glBufferData.xhtml
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertices.size * Float.SIZE_BYTES, vertexBuffer, GLES32.GL_STATIC_DRAW)

        // Xác định kiểu dữ liệu và cách thức đọc dữ liệu cho thuộc tính đỉnh có location = index
        // glVertexAttribPointer(GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, GLint offset);
        // indx: Location của thuộc tính trong VertexShader
        // size: Số lượng thuộc tính truyền vào, ở case này là 3 (x, y, z)
        // type: Kiểu dữ liệu
        // normalized: true -> Chuẩn hóa dữ liệu (tức ánh xạ dữ liệu về phạm vi [-1, 1]) tùy theo kiểu dữ liệu được set. VD: GL_INT -> giá trị các thuộc tính x, y, z, w của đỉnh = chính nó /  INT.MAX_VALUE
        // stride: Khoảng cách (tính bằng bytes) từ vị trí bắt đầu đọc dữ liệu đỉnh đầu tiên đến đỉnh tiếp theo. VD: dữ liệu đỉnh gồm x, y, z, r, g, b, a ->  nếu kiểu dữ liệu của dữ liệu đỉnh là Float thì stride = 7 * Float.SIZE_BYTES
        // offset: Khoảng cách (tính bằng bytes) từ vị trí đầu của mảng dữ liệu đỉnh đến vị trí bắt đầu đọc dữ liệu đỉnh
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
        // Bật thuộc tính đỉnh có location = index để sử dụng
        GLES32.glEnableVertexAttribArray(0)

        // Tương tự, xác định kiểu dữ liệu và cách thức đọc dữ liệu cho thuộc tính màu sắc
        GLES32.glVertexAttribPointer(1, 4, GLES32.GL_FLOAT, false, 7 * Float.SIZE_BYTES, 3 * Float.SIZE_BYTES)
        GLES32.glEnableVertexAttribArray(1)

        // Liên kết EBO và truyền dữ liệu vào
        val indicesBuffer: IntBuffer = ByteBuffer
            .allocateDirect(indices.size * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        indicesBuffer.put(indices)
        indicesBuffer.position(0)
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, EBO)
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, indices.size * Int.SIZE_BYTES, indicesBuffer, GLES32.GL_STATIC_DRAW)

        // Hủy liên kết VBO và VAO khi không sử dụng nữa
        // 0: Hủy liên kết tất cả buffer đc liên kết trước đó
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0)
        GLES32.glBindVertexArray(0)
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Tính tỷ lệ màn hình
        val aspectRatio = if (width > height) width.toFloat() / height else height.toFloat() / width

        // eyeX, eyeY, eyeZ: Vị trí của camera
        // centerX, centerY, centerZ: Hướng camera nhìn vào
        // upX, upY, upZ: Xác định đâu là trục hướng lên của camera
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, -3f,
            0f, 0f, 0f,
            0f, 1f, 0f)
        GLES32.glUniformMatrix4fv(1, 1, false, viewMatrix, 0)

        // Tạo ma trận chiếu trực giao
        // Tùy vào orientation hiện tại của màn hình, đặt chiều ngắn hơn trong phạm vi [-1, 1] và chiều còn lại trong phạm vi [-aspectRatio, aspectRatio]
//        if (width > height) {
//            Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -2f, 2f)
//        } else {
//            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -2f, 2f)
//        }

        // fovy: Phạm vi nhìn thấy theo chiều dọc (tính bằng độ)
        // aspect: Tỉ lệ chiều rộng / chiều cao
        // zNear: Khoảng cách từ camera đến mặt phẳng gần
        // zFar: Khoảng cách từ camera đến mặt phẳng xa
        Matrix.perspectiveM(projectionMatrix, 0, 45f, 1/aspectRatio, 0.1f, 10f)

        // Để lấy ra location của uniform hoặc attribute mà ko cần set layout trong shader, glGet....Location(tên attribute hoặc uniform)
        val uniformLocation = GLES32.glGetUniformLocation(program, "projectionMatrix")
        // Gán giá trị ma trận chiếu cho uniform projectionMatrix
        // uniformLocation: Vị trí của uniform
        // 1: Số lượng ma trận truyền vào
        // false: Ma trận có phải là ma trận chuyển vị hay ko (tức hàng được đổi thành cột và ngược lại)
        // projectionMatrix: Dữ liệu được truyền vào biến uniform
        // 0: Offset để biết bắt đầu đọc dữ liệu mảng chứa ma trận chiếu từ vị trí nào
        GLES32.glUniformMatrix4fv(uniformLocation, 1, false, projectionMatrix, 0)

        // glViewport set phạm vi hiển thị của OpenGL
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear bộ đệm màu sắc để thay bằng màu mới đã chỉ định ở onSurfaceCreated
        // Tiện thể clear luôn bộ đệm z
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Tăng thời gian để tạo hiệu ứng liên tục
        timeElapsed += animationSpeed * 0.016f // Giả 60 FPS (thời gian cho mỗi khung hình là 1/60 ≈ 0,016f)
        if (timeElapsed > 1.0f) {
            timeElapsed -= 1.0f
        } else if (timeElapsed < 0.0f) {
            timeElapsed += 1.0f
        }

        // Biến đổi ma trận được truyền vào thành ma trận định danh
        Matrix.setIdentityM(modelMatrix, 0)

        Matrix.setIdentityM(rotationMatrix, 0)
        // Biến đổi ma trận định danh thành ma trận xoay
        // Matrix.setRotateM(float[] rm, int rmOffset, float a, float x, float y, float z)
        // rm: Ma trận cần biến đổi
        // rmOffset: offset để đọc ma trận
        // a (angle): góc xoay
        // x: Tọa độ x của điểm cuối nối với tâm đối tượng để tạo vectơ được đặt làm trục xoay
        // y: Tọa độ y của điểm cuối nối với tâm đối tượng để tạo vectơ được đặt làm trục xoay
        // y: Tọa độ z của điểm cuối nối với tâm đối tượng để tạo vectơ được đặt làm trục xoay
        Matrix.setRotateM(rotationMatrix, 0, timeElapsed * 360.0f, 0.0f, 1.0f, 0.0f)

        // Tương tự thêm ma trận dịch chuyển
        Matrix.setIdentityM(translateMatrix, 0)
        Matrix.translateM(translateMatrix, 0, 0f, 0f, timeElapsed * 3f)

        // Nhân lại với nhau và truyền kết quả vào modelMatrix (Chú ý thứ tự (scale -> rotation -> translate)
        Matrix.multiplyMM(modelMatrix, 0, translateMatrix, 0, rotationMatrix, 0)

        // Set giá trị cho modelMatrix
        GLES32.glUniformMatrix4fv(0, 1, false, modelMatrix, 0)

        // Liên kết VAO và vẽ tam giác
        GLES32.glBindVertexArray(VAO)
        // Kết xuất các hình nguyên thủy (điểm, đường, tam giác) từ mảng dữ liệu
        // glDrawArrays(GLenum mode, GLint first, GLsizei count);
        // mode: kiểu hình nguyên thủy
        // first: Vị trí bắt đầu của mảng dữ liệu đỉnh đã được enable
        // count: Số lượng đỉnh được vẽ
//        GLES32.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        // glDrawElements(int var0, int var1, int var2, int var3);
        // var0: kiểu hình nguyên thủy
        // var1: Số lượng đỉnh được vẽ
        // var2: Kiểu dữ liệu của indices
        // var3: offset (int)
        GLES32.glDrawElements(GLES32.GL_TRIANGLES, indices.size, GLES32.GL_UNSIGNED_INT, 0)
        // Hủy liên kết VAO khi ko dùng đến nữa
        GLES32.glBindVertexArray(0)
    }
}