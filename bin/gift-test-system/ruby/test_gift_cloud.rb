require 'minitest/autorun'
require 'minitest/reporters'
require 'shoulda/context'

class TestGiftCloud < Minitest::Test
  context 'Upload client' do
    context '(before any operation)' do
      should 'sign in' do
        skip 'not implemented'
      end
    end
    
    context '(after sign-in)' do
      # PROJECT ==========================================
      should 'create a new project' do
        skip 'not implemented'
      end
      
      should 'not re-create an existing project' do
        skip 'not implemented'
      end
      
      should 'list all projects accessible to user' do
        skip 'not implemented'
      end
      
      should 'not list any project not accessible to user' do
        skip 'not implemented'
      end
      # ==================================================
      
      # SUBJECT ==========================================
      should 'create a new subject' do
        skip 'not implemented'
      end
      
      should 'not re-create an existing subject' do
        skip 'not implemented'
      end
      
      should 'list all subjects in a project' do
        skip 'not implemented'
      end
      # ==================================================
      
      # UPLOAD ===========================================
      should 'upload zipped DICOM studies of a subject to a new session' do
        skip 'not implemented'
      end
      
      should 'upload zipped DICOM studies of a subject to an existing session' do
        skip 'not implemented'
      end
      # ==================================================
      
      # PSEUDONYM ========================================
      should 'create a new pseudonym for a subject' do
        skip 'not implemented'
      end
      
      should 'retrieve subject corresponding to an existing pseudonym' do
        skip 'not implemented'
      end
      
      should 'not re-create an existing pseudonym' do
        skip 'not implemented'
      end
      # ==================================================
    end
  
    context '(after all operations)' do
      should 'sign out' do
        skip 'not implemented'
      end
    end
  end
end