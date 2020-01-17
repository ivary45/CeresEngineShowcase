package ceresgame.main;

import ceresgame.audio.AudioLoop;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import ceresgame.graphics.DisplayUpdater;
import ceresgame.graphics.Renderer;
import ceresgame.graphics.gui.Camera;
import ceresgame.helpers.VectorMath;
import ceresgame.main.userinterface.Input;
import ceresgame.map.GraphicalComponent;
import ceresgame.map.Player;
import ceresgame.models.RawModel;
import ceresgame.models.TexturedModel;
import ceresgame.shaders.StaticShader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

/**
*The main class containing the runner object which contains all the relevant objects, so they can all be connected together (avoiding static issues)
*
*/
public class CeresStation{
	
    //initialize graphical components
    private Player player;
    private GraphicalComponent background;
    private GraphicalComponent foreground;
    //private God god;
    
    //initialize threads
    private Input inputThread;
    private AudioLoop audioThread;
    private Camera camera;
	
    private final float[] imageUVVerticies = {
	//Top left point
    	0,0,
	//Bottom left point
	0,1,
	//Bottom right point
	1,1,
	//Top right point
	1,0
    };

    private final int[] indiciesForRendering = {
	//Top left triangle
    	0,1,3,
	//Bottom right triangle
    	3,1,2
    };
    
    private ArrayList<Integer> vaos = new ArrayList<>();
    private ArrayList<Integer> vbos = new ArrayList<>();
    private ArrayList<Integer> textures = new ArrayList<>();
    
    private ArrayList<GraphicalComponent> components = new ArrayList<>();
    private StaticShader shader;
    private Renderer renderer;
    
    //private Camera camera = new Camera(this); //Please feed in a CeresStation object so you can reference the player
	
    /**
    *The constructor of the main game object
    *
    */
    public CeresStation() {
    	start();
    }
    
    /**
    *The method which starts all relevant processes on startup.
    *
    */
    public void start() {
        
        //create textures for graphical components
        //ceresgame.textures.ObjectTexture godTexture = new ceresgame.textures.ObjectTexture(loadTexture("resources/images/God.png"));
        
        //create the objects out of the graphical components
    	player = genPlayer(new Vector3f(0, -0.2f, -1), 0.2f, 0.2f, "resources/images/Ariff.png");
        background = genGraphicalComponent(new Vector3f(1.1f,-0.4f,-1.5f), 8f, 4f, "resources/images/Background.png");
        foreground = genGraphicalComponent(new Vector3f(0.26f, -0.05f,-0.5f), 2.2f, 2f, "resources/images/snowforeground.png");
        
        //List components from back to front for alpha blending to work
        components.add(background);
        components.add(player);
        components.add(foreground);
        
    	inputThread = new Input(this, player);
    	audioThread = new AudioLoop(this);
    	
    	inputThread.start();
        audioThread.start();
    }
    
    /**
    *The method which closes all relevant processes on program close.
    *
    */
    public void close() {
    	inputThread.stop();
    	audioThread.delete();
        audioThread.stop();
        
        this.deleteVAOVBOTEXTURE();
        shader.delete();
        
        
      //TODO: Add all delete methods here^^
    }
        
	public static void main(String[] args) {
		DisplayUpdater.createDisplay();
		
                
                CeresStation game = new CeresStation();
                game.camera = new Camera();
		game.shader = new StaticShader();
                game.renderer = new Renderer(game.shader);
                
		while(!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_F)) {
			//Update saved positions of graphicalComponents
			game.renderer.prepare();
			game.shader.start();
                        game.shader.loadViewMatrix(game.camera);
			game.render(game.renderer, game.shader);
			game.shader.stop();
                        
			DisplayUpdater.updateDisplay();
		}
		
		game.close();
		
		
		DisplayUpdater.closeDisplay();
	}
        
        private Player genPlayer(Vector3f position, float width, float height, String path){
            ceresgame.textures.ObjectTexture texture = new ceresgame.textures.ObjectTexture(loadTexture(path));
            float[] playerVerticies = VectorMath.genVertices(VectorMath.genVector(position.getX(), position.getY(), position.getZ(), width, height), width, height);
            RawModel rawModel = generateRawModel(playerVerticies, imageUVVerticies, indiciesForRendering);
            TexturedModel model = new TexturedModel(rawModel, texture);
            Player component = new Player(position.getX(), position.getY(), position.getZ(), width, height, model);
            return component;
        }
        
        private GraphicalComponent genGraphicalComponent(Vector3f position, float width, float height, String path){
            ceresgame.textures.ObjectTexture texture = new ceresgame.textures.ObjectTexture(loadTexture(path));
            float[] playerVerticies = VectorMath.genVertices(VectorMath.genVector(position.getX(), position.getY(), position.getZ(), width, height), width, height);
            RawModel rawModel = generateRawModel(playerVerticies, imageUVVerticies, indiciesForRendering);
            TexturedModel model = new TexturedModel(rawModel, texture);
            GraphicalComponent component = new GraphicalComponent(position.getX(), position.getY(), position.getZ(), width, height, model);
            return component;
        }
	
	/**
	*The method which gets the player object
	*@return The player object
	*/
	public Player getPlayer(){
            return this.player;	
	}
        
        /**
         * The method which gets the camera object
         * @return The camera object
         */
        public Camera getCamera(){
            return this.camera;
        }
	
	/**
	*The method which gets the input thread object
	*@return The input thread object
	*/
	public Input getInput() {
		return this.inputThread;
	}
	
	/**
	*The method which gets the audio thread object
	*@return The audio thread object
	*/
	public AudioLoop getAudioLoop() {
		return this.audioThread;
	}
	
	/**
	*Adds graphical components to the list of components being used
	*@param gc The graphical component being added
	*/
        public void addComponent(GraphicalComponent gc) {
            components.add(gc);
        }
	
	/**
	*Renders all graphicalComponents in the list
	*@param renderer The renderer used to render the graphical components
	*@param shader The shader used to position the graphical components onto the visual plane
	*/
	public void render(Renderer renderer, StaticShader shader){
            //renderer.render(player, shader);
            for(int i = 0; i < components.size(); i++){
                renderer.render(components.get(i), shader);
            }
	}
        
        private RawModel generateRawModel(float[] position, float[] textureCoords, int[] indicies) {
            int vaoID = createVAO();
	    bindIndicesBuffer(indicies);
            storeAttributeData(0, 3, position);
            storeAttributeData(1, 2, textureCoords);
            unbindVAO();
            return new RawModel(vaoID, indicies.length);
        }

        private int createVAO() {
            int vaoID = GL30.glGenVertexArrays();
            vaos.add(vaoID);
            GL30.glBindVertexArray(vaoID);
            return vaoID;
        }

        private void storeAttributeData(int attributeNumber, int coordinateSize, float[] verticies) {
            System.out.println("Storing attribute array of size: " + coordinateSize);
            int vboID = GL15.glGenBuffers();
            vbos.add(vboID);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
            FloatBuffer buffer = storeVerticiesInFloatBuffer(verticies);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, 0, 0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            System.out.println("Attrubute array stored in position: " + attributeNumber);
        }

        private void unbindVAO() {
            GL30.glBindVertexArray(0);
        }
	
	private void bindIndicesBuffer(int[] indicies){
	    int vboID = GL15.glGenBuffers();
	    vbos.add(vboID);
	    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboID);
	    IntBuffer buffer = storeVerticiesInIntBuffer(indicies);
	    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
	}
	
	private IntBuffer storeVerticiesInIntBuffer(int[] verticies){
		IntBuffer buffer = BufferUtils.createIntBuffer(verticies.length);
		buffer.put(verticies);
            	buffer.flip();
            	return buffer;
	}
        
        private FloatBuffer storeVerticiesInFloatBuffer(float[] verticies){
            FloatBuffer buffer = BufferUtils.createFloatBuffer(verticies.length);
            buffer.put(verticies);
            buffer.flip();
            return buffer;
        }
        
        private void deleteVAOVBOTEXTURE(){
            vaos.forEach((vao) -> {
                GL30.glDeleteVertexArrays(vao);
            });
            vbos.forEach((vbo) -> {
                GL15.glDeleteBuffers(vbo);
            });
            textures.forEach((texture) -> {
                GL11.glDeleteTextures(texture);
            });
        }
        
        private int loadTexture(String path){
            Texture texture = null;
            try {
                texture = TextureLoader.getTexture("png", new FileInputStream(path));
            } catch (FileNotFoundException ex) {
                System.out.println("Image: " + path + " cannot be found!");
            } catch (IOException ex) {
                System.out.println("IO Error loading: " + path);
            }
            textures.add(texture.getTextureID());
            return texture.getTextureID();
        }
        

    
}
